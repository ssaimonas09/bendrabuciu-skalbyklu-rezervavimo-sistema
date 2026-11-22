import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class WebServerIntegrationTest {
    @Test
    void serverChecksLoginRoleTenantAndSettings() throws Exception {
        String url = System.getenv("JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "Reikalingas PostgreSQL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        PostgresReservationSettingsSource settings =
                new PostgresReservationSettingsSource(url, dbUser, dbPassword);
        PostgresSettingsAdmin admin = new PostgresSettingsAdmin(url, dbUser, dbPassword);
        int originalA = settings.findForDormitory(1).durationMinutes();
        int originalB = settings.findForDormitory(2).durationMinutes();
        int changedA = originalA == 90 ? 60 : 90;
        PostgresReservationRepository repository =
                new PostgresReservationRepository(url, dbUser, dbPassword);
        Reservation first = null;
        Reservation second = null;
        HttpServer server = new WebServer(url, dbUser, dbPassword).start(0);
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            HttpClient client = HttpClient.newHttpClient();
            assertEquals(401, get(client, base + "/api/reservations", null));
            assertEquals(401, get(client, base + "/api/reservations", "student_1_101:wrong"));
            assertEquals(403, get(client, base + "/api/reservations?dormitoryId=2",
                    "student_1_101:student_demo"));
            assertEquals(403, get(client, base
                    + "/api/availability?machineId=2&date=2030-11-20&dormitoryId=2",
                    "student_1_101:student_demo"));
            assertEquals(200, get(client, base + "/api/reservations?dormitoryId=1",
                    "student_1_101:student_demo"));
            assertEquals(403, post(client, base + "/admin/settings",
                    "student_1_101:student_demo", "minutes=90&dormitoryId=1"));
            assertEquals(403, post(client, base + "/admin/settings",
                    "admin_1:admin_demo", "minutes=90&dormitoryId=2"));
            assertEquals(400, post(client, base + "/admin/settings",
                    "admin_1:admin_demo", "minutes=961&dormitoryId=1"));
            assertEquals(originalA, settings.findForDormitory(1).durationMinutes());
            assertEquals(200, post(client, base + "/admin/settings",
                    "admin_1:admin_demo", "minutes=" + changedA + "&dormitoryId=1"));
            assertEquals(changedA, settings.findForDormitory(1).durationMinutes());
            assertEquals(originalB, settings.findForDormitory(2).durationMinutes());

            LocalDateTime start = LocalDate.now().plusYears(40).atTime(15, 0);
            ReservationService service = new ReservationService(repository,
                    java.time.Clock.systemDefaultZone(), settings);
            first = service.createReservation(new User(101, "A", 1),
                    new WashingMachine(2, 1), start);
            second = service.createReservation(new User(101, "B", 2),
                    new WashingMachine(2, 2), start);
            assertEquals(start.plusMinutes(changedA), first.getEndTime());
            assertEquals(start.plusMinutes(originalB), second.getEndTime());
        } finally {
            if (first != null) repository.delete(first);
            if (second != null) repository.delete(second);
            admin.changeDuration(1, originalA);
            server.stop(0);
        }
    }

    @Test
    void residentCanBookReadAndCancelThroughHttp() throws Exception {
        String url = System.getenv("JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "Reikalingas PostgreSQL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        PostgresReservationRepository repository =
                new PostgresReservationRepository(url, dbUser, dbPassword);
        LocalDateTime start = LocalDate.now().plusYears(25).atTime(15, 0);
        HttpServer server = new WebServer(url, dbUser, dbPassword).start(0);
        Reservation created = null;
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            HttpClient client = HttpClient.newHttpClient();
            assertEquals(400, post(client, base + "/reservations",
                    "student_1_101:student_demo", "machineId=2&date="
                            + start.toLocalDate() + "&time=25%3A00"));
            assertEquals(400, post(client, base + "/reservations",
                    "student_1_101:student_demo", "machineId=2&date="
                            + start.toLocalDate() + "&hour=13&minute=33"));
            assertEquals(200, post(client, base + "/reservations",
                    "student_1_101:student_demo", "machineId=2&date="
                            + start.toLocalDate() + "&hour=15&minute=00"));
            created = repository.findByDormitoryId(1).stream()
                    .filter(item -> item.getUserId() == 101
                            && item.getWashingMachineId() == 2
                            && item.getStartTime().equals(start))
                    .findFirst().orElseThrow();
            HttpRequest conflict = HttpRequest.newBuilder(URI.create(base + "/reservations"))
                    .header("Cookie", loginCookie(client, base, "student_1_101:student_demo"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("machineId=2&date="
                            + start.toLocalDate() + "&hour=15&minute=00")).build();
            HttpResponse<String> rejected = client.send(conflict,
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(400, rejected.statusCode());
            assertTrue(rejected.headers().firstValue("Content-Type").orElseThrow()
                    .startsWith("text/html"));
            assertTrue(rejected.body().contains("Pasirinktas laikas jau užimtas."));
            assertTrue(rejected.body().contains("action='/reservations'"));
            assertTrue(rejected.body().contains("href='/'"));
            String home = getWithCookie(client, base + "/",
                    loginCookie(client, base, "student_1_101:student_demo")).body();
            assertTrue(home.contains("type='date' name='date'"));
            assertTrue(home.contains("<select name='hour' required>"));
            assertTrue(home.contains("<select name='minute' required>"));
            assertTrue(home.contains("<option value='30'>30</option>"));
            assertTrue(!home.contains("<option value='33'>33</option>"));
            assertTrue(!home.contains("pvz."));
            assertTrue(home.contains(start.toLocalDate() + " 15:00–"));
            assertTrue(!home.contains(start.toLocalDate() + "T15:00"));

            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/reservations"))
                    .header("Authorization", basic("student_1_101:student_demo"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"id\":" + created.getId()));
            assertEquals(200, post(client, base + "/cancel",
                    "student_1_101:student_demo", "reservationId=" + created.getId()));
            int id = created.getId();
            assertTrue(repository.findByDormitoryId(1).stream()
                    .noneMatch(item -> item.getId() == id));
        } finally {
            if (created != null) repository.delete(created);
            server.stop(0);
        }
    }

    @Test
    void administratorCreatesResidentOnlyInOwnDormitory() throws Exception {
        String url = System.getenv("JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "Reikalingas PostgreSQL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        String login = "test_" + UUID.randomUUID().toString().substring(0, 8);
        String secondLogin = "test_" + UUID.randomUUID().toString().substring(0, 8);
        String form = "name=Testas&login=" + login + "&password=test_password_123";
        HttpServer server = new WebServer(url, dbUser, dbPassword).start(0);
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            HttpClient client = HttpClient.newHttpClient();
            assertEquals(403, post(client, base + "/admin/residents",
                    "student_1_101:student_demo", form));
            assertEquals(403, post(client, base + "/admin/residents",
                    "admin_1:admin_demo", form + "&dormitoryId=2"));
            assertEquals(200, post(client, base + "/admin/residents",
                    "admin_1:admin_demo", form));

            Account created = new PostgresAccounts(url, dbUser, dbPassword)
                    .authenticate(login, "test_password_123");
            assertEquals(1, created.user().getDormitoryId());
            assertEquals("RESIDENT", created.role());
            assertEquals(403, get(client, base + "/api/reservations?dormitoryId=2",
                    login + ":test_password_123"));
            assertEquals(400, post(client, base + "/admin/residents",
                    "admin_1:admin_demo", form));
            assertEquals(200, post(client, base + "/admin/residents",
                    "admin_1:admin_demo", "name=Kitas&login=" + secondLogin
                            + "&password=test_password_123"));
            Account second = new PostgresAccounts(url, dbUser, dbPassword)
                    .authenticate(secondLogin, "test_password_123");
            assertTrue(second.user().getId() != created.user().getId());
        } finally {
            try (var connection = DriverManager.getConnection(url, dbUser, dbPassword);
                 var statement = connection.prepareStatement(
                         "DELETE FROM users WHERE login IN (?, ?)")) {
                statement.setString(1, login);
                statement.setString(2, secondLogin);
                statement.executeUpdate();
            }
            server.stop(0);
        }
    }

    @Test
    void browserCanSwitchAccountsAndLogout() throws Exception {
        String url = System.getenv("JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "Reikalingas PostgreSQL");
        HttpServer server = new WebServer(url, System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD")).start(0);
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            HttpClient client = HttpClient.newHttpClient();
            assertEquals(303, get(client, base + "/", null));
            assertEquals(200, get(client, base + "/login", null));
            HttpResponse<String> wrong = client.send(HttpRequest.newBuilder(URI.create(base + "/login"))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "login=student_1_101&password=wrong"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, wrong.statusCode());
            assertTrue(wrong.body().contains("Neteisingas"));

            String firstCookie = loginCookie(client, base, "student_1_101:student_demo");
            assertEquals(200, getWithCookie(client, base + "/", firstCookie).statusCode());
            String firstPage = getWithCookie(client, base + "/", firstCookie).body();
            assertTrue(firstPage.contains("Bendrabutis Nr. 1"));
            assertTrue(firstPage.contains("name='hour'"));
            assertTrue(firstPage.contains("name='minute'"));
            assertEquals(403, getWithCookie(client,
                    base + "/api/reservations?dormitoryId=2", firstCookie).statusCode());

            HttpResponse<String> logout = client.send(HttpRequest.newBuilder(URI.create(base + "/logout"))
                    .header("Cookie", firstCookie)
                    .POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(303, logout.statusCode());
            assertEquals("/login", logout.headers().firstValue("Location").orElseThrow());
            assertEquals(303, getWithCookie(client, base + "/", firstCookie).statusCode());
            assertEquals(401, getWithCookie(client,
                    base + "/api/reservations", firstCookie).statusCode());

            String secondCookie = loginCookie(client, base, "student_2_101:student_demo");
            assertTrue(getWithCookie(client, base + "/", secondCookie).body().contains("Bendrabutis Nr. 2"));
            assertEquals(403, getWithCookie(client,
                    base + "/api/reservations?dormitoryId=1", secondCookie).statusCode());
        } finally {
            server.stop(0);
        }
    }

    private static int get(HttpClient client, String url, String credentials) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
        if (credentials != null) request.header("Authorization", basic(credentials));
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static int post(HttpClient client, String url, String credentials, String body)
            throws Exception {
        String cookie = loginCookie(client, url.substring(0, url.indexOf('/', 8)), credentials);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Cookie", cookie)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private static String loginCookie(HttpClient client, String base, String credentials)
            throws Exception {
        int separator = credentials.indexOf(':');
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("login=" + credentials.substring(0, separator)
                        + "&password=" + credentials.substring(separator + 1))).build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        assertEquals(303, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private static HttpResponse<String> getWithCookie(HttpClient client, String url, String cookie)
            throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).header("Cookie", cookie)
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String basic(String credentials) {
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }
}
