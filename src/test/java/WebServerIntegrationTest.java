import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class WebServerIntegrationTest {
    private static final int DORMITORY_A_ID = 1;
    private static final int DORMITORY_B_ID = 2;
    private static final int STUDENT_ID = 101;
    private static final int MACHINE_ID = 2;
    private static final String STUDENT_CREDENTIALS = "student_1_101:student_demo";
    private static final String ADMIN_CREDENTIALS = "admin_1:admin_demo";

    private final String url = System.getenv("JDBC_URL");
    private final String dbUser = System.getenv("DB_USER");
    private final String dbPassword = System.getenv("DB_PASSWORD");
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpServer server;
    private String base;

    @BeforeEach
    void startServer() throws Exception {
        assumeTrue(url != null && !url.isBlank(), "Reikalingas PostgreSQL");
        server = new WebServer(url, dbUser, dbPassword).start(0);
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void enforcesApiAccessAndAppliesDormitoryDurationChange() throws Exception {
        PostgresReservationSettingsRepository settings =
                new PostgresReservationSettingsRepository(url, dbUser, dbPassword);
        int originalA = settings.findForDormitory(DORMITORY_A_ID).durationMinutes();
        int originalB = settings.findForDormitory(DORMITORY_B_ID).durationMinutes();
        int changedA = originalA == 90 ? 60 : 90;
        PostgresReservationRepository repository =
                new PostgresReservationRepository(url, dbUser, dbPassword);
        Reservation first = null;
        Reservation second = null;
        try {
            assertEquals(401, getApiResponse("/api/reservations", null).statusCode());
            assertEquals(401, getApiResponse("/api/reservations", "student_1_101:wrong").statusCode());
            assertEquals(403, getApiResponse("/api/reservations?dormitoryId=2",
                    STUDENT_CREDENTIALS).statusCode());
            assertEquals(403, getApiResponse("/api/availability?machineId=2&date=2030-11-20&dormitoryId=2",
                    STUDENT_CREDENTIALS).statusCode());
            assertEquals(200, getApiResponse("/api/reservations?dormitoryId=1",
                    STUDENT_CREDENTIALS).statusCode());
            assertEquals(403, postFormStatusAs("/admin/settings",
                    STUDENT_CREDENTIALS, "minutes=90&dormitoryId=1"));
            assertEquals(403, postFormStatusAs("/admin/settings",
                    ADMIN_CREDENTIALS, "minutes=90&dormitoryId=2"));
            assertEquals(400, postFormStatusAs("/admin/settings",
                    ADMIN_CREDENTIALS, "minutes=961&dormitoryId=1"));
            assertEquals(originalA, settings.findForDormitory(DORMITORY_A_ID).durationMinutes());
            assertEquals(200, postFormStatusAs("/admin/settings",
                    ADMIN_CREDENTIALS, "minutes=" + changedA + "&dormitoryId=1"));
            assertEquals(changedA, settings.findForDormitory(DORMITORY_A_ID).durationMinutes());
            assertEquals(originalB, settings.findForDormitory(DORMITORY_B_ID).durationMinutes());

            LocalDateTime start = LocalDate.now().plusYears(40).atTime(15, 0);
            ReservationService service = new ReservationService(repository,
                    Clock.systemDefaultZone(), settings);
            first = service.createReservation(new User(STUDENT_ID, "A", DORMITORY_A_ID),
                    new WashingMachine(MACHINE_ID, DORMITORY_A_ID), start);
            second = service.createReservation(new User(STUDENT_ID, "B", DORMITORY_B_ID),
                    new WashingMachine(MACHINE_ID, DORMITORY_B_ID), start);
            assertEquals(start.plusMinutes(changedA), first.getEndTime());
            assertEquals(start.plusMinutes(originalB), second.getEndTime());
        } finally {
            if (first != null) repository.delete(first);
            if (second != null) repository.delete(second);
            settings.updateReservationDuration(DORMITORY_A_ID, originalA);
        }
    }

    @Test
    void residentCanCheckAvailabilityBookAndCancel() throws Exception {
        PostgresReservationRepository repository =
                new PostgresReservationRepository(url, dbUser, dbPassword);
        LocalDateTime start = LocalDate.now().plusYears(25).atTime(15, 0);
        String cookie = signInAndGetCookie(STUDENT_CREDENTIALS);
        String machineAndDate = "machineId=" + MACHINE_ID + "&date=" + start.toLocalDate();
        String availabilityPath = "/api/availability?" + machineAndDate;
        String reservationForm = machineAndDate + "&hour=15&minute=00";
        String requestedStart = "\"" + start + "\"";
        Reservation created = null;
        try {
            HttpResponse<String> availableBeforeBooking = getApiResponse(availabilityPath, STUDENT_CREDENTIALS);
            assertEquals(200, availableBeforeBooking.statusCode());
            assertTrue(availableBeforeBooking.body().contains(requestedStart));

            assertEquals(400, postForm("/reservations", cookie, machineAndDate + "&time=25%3A00").statusCode());
            assertEquals(400, postForm("/reservations", cookie, machineAndDate + "&hour=13&minute=33").statusCode());
            assertEquals(200, postForm("/reservations", cookie, reservationForm).statusCode());
            created = repository.findByDormitoryId(DORMITORY_A_ID).stream()
                    .filter(item -> item.getUserId() == STUDENT_ID
                            && item.getWashingMachineId() == MACHINE_ID
                            && item.getStartTime().equals(start))
                    .findFirst().orElseThrow();

            HttpResponse<String> rejected = postForm("/reservations", cookie, reservationForm);
            assertEquals(400, rejected.statusCode());
            assertTrue(rejected.body().contains("Pasirinktas laikas jau užimtas."));

            HttpResponse<String> availableAfterBooking = getApiResponse(availabilityPath, STUDENT_CREDENTIALS);
            assertEquals(200, availableAfterBooking.statusCode());
            assertFalse(availableAfterBooking.body().contains(requestedStart));

            int id = created.getId();
            HttpResponse<String> reservations = getApiResponse("/api/reservations", STUDENT_CREDENTIALS);
            assertEquals(200, reservations.statusCode());
            assertTrue(reservations.body().contains("\"id\":" + id + ","));

            assertEquals(200, postForm("/cancel", cookie, "reservationId=" + id).statusCode());
            assertFalse(repository.findByDormitoryId(DORMITORY_A_ID).stream()
                    .anyMatch(reservation -> reservation.getId() == id));
        } finally {
            if (created != null) repository.delete(created);
        }
    }

    @Test
    void logoutInvalidatesSessionCookie() throws Exception {
        String cookie = signInAndGetCookie(STUDENT_CREDENTIALS);
        HttpRequest reservations = HttpRequest.newBuilder(URI.create(base + "/api/reservations"))
                .header("Cookie", cookie).GET().build();
        assertEquals(200, client.send(reservations, HttpResponse.BodyHandlers.discarding()).statusCode());

        HttpRequest logout = HttpRequest.newBuilder(URI.create(base + "/logout"))
                .header("Cookie", cookie).POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(303, client.send(logout, HttpResponse.BodyHandlers.discarding()).statusCode());
        assertEquals(401, client.send(reservations, HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    private HttpResponse<String> getApiResponse(String path, String credentials) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path)).GET();
        if (credentials != null) {
            request.header("Authorization", basicAuthorization(credentials));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private int postFormStatusAs(String path, String credentials, String form) throws Exception {
        return postForm(path, signInAndGetCookie(credentials), form).statusCode();
    }

    private HttpResponse<String> postForm(String path, String cookie, String form) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .header("Cookie", cookie)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String signInAndGetCookie(String credentials) throws Exception {
        int separator = credentials.indexOf(':');
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("login=" + credentials.substring(0, separator)
                        + "&password=" + credentials.substring(separator + 1))).build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        assertEquals(303, response.statusCode());
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private static String basicAuthorization(String credentials) {
        return "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
    }
}
