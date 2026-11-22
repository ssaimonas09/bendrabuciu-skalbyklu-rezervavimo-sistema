import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer {
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final String SESSION_COOKIE = "laundry_session";
    private static final long SESSION_SECONDS = 8 * 60 * 60;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final PostgresAccounts accounts;
    private final PostgresCatalog catalog;
    private final PostgresSettingsAdmin adminSettings;
    private final ReservationSettingsSource settings;
    private final ReservationService service;
    private final ReservationStore reservations;

    public WebServer(String url, String dbUser, String dbPassword) {
        accounts = new PostgresAccounts(url, dbUser, dbPassword);
        catalog = new PostgresCatalog(url, dbUser, dbPassword);
        adminSettings = new PostgresSettingsAdmin(url, dbUser, dbPassword);
        settings = new PostgresReservationSettingsSource(url, dbUser, dbPassword);
        reservations = new PostgresReservationRepository(url, dbUser, dbPassword);
        service = new ReservationService(reservations, Clock.systemDefaultZone(), settings);
    }

    public static void main(String[] args) throws IOException {
        String url = System.getenv("JDBC_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Nenurodytas JDBC_URL.");
        }
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Nerasta PostgreSQL JDBC tvarkykle.", exception);
        }
        String portText = System.getenv().getOrDefault("PORT", "8080");
        WebServer app = new WebServer(url, System.getenv("DB_USER"),
                System.getenv("DB_PASSWORD"));
        app.start(Integer.parseInt(portText));
        System.out.println("Skalbyklos svetaine veikia, portas " + portText);
    }

    public HttpServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.start();
        return server;
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Account account = null;
        try {
            String method = exchange.getRequestMethod();
            if (method.equals("GET") && path.equals("/login")) {
                showLogin(exchange, "");
                return;
            }
            if (method.equals("POST") && path.equals("/login")) {
                login(exchange);
                return;
            }
            if (method.equals("POST") && path.equals("/logout")) {
                logout(exchange);
                return;
            }

            account = sessionAccount(exchange);
            if (account == null && path.startsWith("/api/")) {
                account = authenticateBasic(exchange);
            }
            if (account == null) {
                if (path.startsWith("/api/")) {
                    send(exchange, 401, "text/plain", "Prisijunkite.");
                } else {
                    redirect(exchange, "/login");
                }
                return;
            }

            if (method.equals("GET") && path.equals("/")) {
                showHome(exchange, account, "");
            } else if (method.equals("GET") && path.equals("/availability")) {
                showAvailability(exchange, account);
            } else if (method.equals("GET") && path.equals("/api/reservations")) {
                apiReservations(exchange, account);
            } else if (method.equals("GET") && path.equals("/api/availability")) {
                apiAvailability(exchange, account);
            } else if (method.equals("POST") && path.equals("/reservations")) {
                createReservation(exchange, account);
            } else if (method.equals("POST") && path.equals("/cancel")) {
                cancelReservation(exchange, account);
            } else if (method.equals("POST") && path.equals("/admin/settings")) {
                changeSettings(exchange, account);
            } else if (method.equals("POST") && path.equals("/admin/residents")) {
                createResident(exchange, account);
            } else {
                send(exchange, 404, "text/plain", "Puslapis nerastas.");
            }
        } catch (IllegalArgumentException | DateTimeException exception) {
            String message = exception.getMessage() == null ? "Neteisingi duomenys."
                    : exception.getMessage();
            if (path.startsWith("/api/")) {
                send(exchange, 400, "text/plain", message);
            } else {
                String notice = "<p class='error' role='alert'>" + escape(message)
                        + " <a href='/'>Grįžti į pradžią</a></p>";
                if (account == null) {
                    send(exchange, 400, "text/html", loginPage(notice));
                } else {
                    showHome(exchange, account, notice, 400);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            send(exchange, 500, "text/plain", "Serverio klaida.");
        } finally {
            exchange.close();
        }
    }

    private void showLogin(HttpExchange exchange, String message) throws IOException {
        send(exchange, 200, "text/html", loginPage(message));
    }

    private static String loginPage(String message) throws IOException {
        String body = "<section><h2>Prisijungimas</h2>" + message
                + "<form method='post' action='/login'>"
                + "<label>Prisijungimo vardas <input name='login' autocomplete='username' required autofocus></label>"
                + "<label>Slaptažodis <input name='password' type='password' autocomplete='current-password' required></label>"
                + "<button>Prisijungti</button></form></section>";
        return page(body);
    }

    private void login(HttpExchange exchange) throws IOException {
        Map<String, String> form = fields(readForm(exchange));
        Account account = accounts.authenticate(required(form, "login"), required(form, "password"));
        if (account == null) {
            send(exchange, 401, "text/html", loginPage(
                    "<p>Neteisingas prisijungimo vardas arba slaptažodis.</p>"));
            return;
        }
        String previous = cookie(exchange);
        if (previous != null) sessions.remove(previous);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(account, Instant.now().plusSeconds(SESSION_SECONDS)));
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE + "=" + token
                + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + SESSION_SECONDS);
        redirect(exchange, "/");
    }

    private void logout(HttpExchange exchange) throws IOException {
        String token = cookie(exchange);
        if (token != null) sessions.remove(token);
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE
                + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        redirect(exchange, "/login");
    }

    private Account sessionAccount(HttpExchange exchange) {
        String token = cookie(exchange);
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(token);
            return null;
        }
        return session.account();
    }

    private static String cookie(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String item = part.trim();
            if (item.startsWith(SESSION_COOKIE + "=")) {
                return item.substring(SESSION_COOKIE.length() + 1);
            }
        }
        return null;
    }

    private Account authenticateBasic(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Basic ")) {
            return null;
        }
        try {
            String credentials = new String(Base64.getDecoder().decode(
                    header.substring(6)), StandardCharsets.UTF_8);
            int colon = credentials.indexOf(':');
            if (colon < 0) {
                return null;
            }
            return accounts.authenticate(credentials.substring(0, colon),
                    credentials.substring(colon + 1));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean allowedDormitory(HttpExchange exchange, Account account,
                                     Map<String, String> fields) throws IOException {
        String requested = fields.get("dormitoryId");
        if (requested != null && Integer.parseInt(requested) != account.user().getDormitoryId()) {
            send(exchange, 403, "text/plain", "Kito bendrabučio duomenys nepasiekiami.");
            return false;
        }
        return true;
    }

    private boolean resident(HttpExchange exchange, Account account) throws IOException {
        if (account.isAdmin()) {
            send(exchange, 403, "text/plain", "Veiksmas skirtas gyventojui.");
            return false;
        }
        return true;
    }

    private WashingMachine findMachine(Account account, String machineId) {
        int id = Integer.parseInt(machineId);
        for (WashingMachine machine : catalog.findMachines(account.user().getDormitoryId())) {
            if (machine.getId() == id) {
                return machine;
            }
        }
        throw new IllegalArgumentException("Šiame bendrabutyje tokios mašinos nėra.");
    }

    private void showAvailability(HttpExchange exchange, Account account) throws IOException {
        if (!resident(exchange, account)) return;
        Map<String, String> query = fields(exchange.getRequestURI().getRawQuery());
        if (!allowedDormitory(exchange, account, query)) return;
        WashingMachine machine = findMachine(account, required(query, "machineId"));
        LocalDate date = parseDate(required(query, "date"));
        List<LocalDateTime> times = service.getAvailableStartTimes(account.user(), machine, date);
        StringBuilder result = new StringBuilder("<section><h2>Laisvi laikai</h2><p>");
        result.append("Mašina Nr. ").append(machine.getId()).append(", ")
                .append(date).append("</p><ul class='times'>");
        for (LocalDateTime time : times) {
            result.append("<li>").append(time.format(DISPLAY_TIME)).append("</li>");
        }
        if (times.isEmpty()) result.append("<li>Laisvų laikų nėra.</li>");
        result.append("</ul></section>");
        showHome(exchange, account, result.toString());
    }

    private void createReservation(HttpExchange exchange, Account account) throws IOException {
        if (!resident(exchange, account)) return;
        Map<String, String> form = fields(readForm(exchange));
        if (!allowedDormitory(exchange, account, form)) return;
        WashingMachine machine = findMachine(account, required(form, "machineId"));
        LocalDate date = parseDate(required(form, "date"));
        String timeText = form.containsKey("time") ? required(form, "time")
                : required(form, "hour") + ":" + required(form, "minute");
        LocalTime time = parseTime(timeText);
        LocalDateTime start = date.atTime(time);
        Reservation created = service.createReservation(account.user(), machine, start);
        showHome(exchange, account, "<p class='message'>Sukurta rezervacija Nr. "
                + created.getId() + ".</p>");
    }

    private void cancelReservation(HttpExchange exchange, Account account) throws IOException {
        if (!resident(exchange, account)) return;
        Map<String, String> form = fields(readForm(exchange));
        if (!allowedDormitory(exchange, account, form)) return;
        service.cancelReservation(account.user(),
                Integer.parseInt(required(form, "reservationId")));
        showHome(exchange, account, "<p class='message'>Rezervacija atšaukta.</p>");
    }

    private void changeSettings(HttpExchange exchange, Account account) throws IOException {
        if (!account.isAdmin()) {
            send(exchange, 403, "text/plain", "Reikia administratoriaus teisių.");
            return;
        }
        Map<String, String> form = fields(readForm(exchange));
        if (!allowedDormitory(exchange, account, form)) return;
        adminSettings.changeDuration(account.user().getDormitoryId(),
                Integer.parseInt(required(form, "minutes")));
        showHome(exchange, account, "<p class='message'>Trukmė pakeista.</p>");
    }

    private void createResident(HttpExchange exchange, Account account) throws IOException {
        if (!account.isAdmin()) {
            send(exchange, 403, "text/plain", "Reikia administratoriaus teisių.");
            return;
        }
        Map<String, String> form = fields(readForm(exchange));
        if (!allowedDormitory(exchange, account, form)) return;
        String login = required(form, "login").trim();
        accounts.createResident(account.user().getDormitoryId(),
                required(form, "name"), login, required(form, "password"));
        showHome(exchange, account, "<p class='message'>Gyventojo paskyra sukurta: "
                + escape(login) + ".</p>");
    }

    private void apiReservations(HttpExchange exchange, Account account) throws IOException {
        Map<String, String> query = fields(exchange.getRequestURI().getRawQuery());
        if (!allowedDormitory(exchange, account, query)) return;
        List<Reservation> found = account.isAdmin()
                ? reservations.findByDormitoryId(account.user().getDormitoryId())
                : service.getMyReservations(account.user());
        StringBuilder json = new StringBuilder("[");
        for (Reservation reservation : found) {
            if (json.length() > 1) json.append(',');
            json.append("{\"id\":").append(reservation.getId())
                    .append(",\"machineId\":").append(reservation.getWashingMachineId())
                    .append(",\"start\":\"").append(reservation.getStartTime())
                    .append("\",\"end\":\"").append(reservation.getEndTime()).append("\"}");
        }
        send(exchange, 200, "application/json", json.append(']').toString());
    }

    private void apiAvailability(HttpExchange exchange, Account account) throws IOException {
        if (!resident(exchange, account)) return;
        Map<String, String> query = fields(exchange.getRequestURI().getRawQuery());
        if (!allowedDormitory(exchange, account, query)) return;
        WashingMachine machine = findMachine(account, required(query, "machineId"));
        List<LocalDateTime> times = service.getAvailableStartTimes(account.user(),
                machine, parseDate(required(query, "date")));
        StringBuilder json = new StringBuilder("[");
        for (LocalDateTime time : times) {
            if (json.length() > 1) json.append(',');
            json.append('"').append(time).append('"');
        }
        send(exchange, 200, "application/json", json.append(']').toString());
    }

    private void showHome(HttpExchange exchange, Account account, String message) throws IOException {
        showHome(exchange, account, message, 200);
    }

    private void showHome(HttpExchange exchange, Account account, String message, int status)
            throws IOException {
        User user = account.user();
        StringBuilder body = new StringBuilder("<div class='account-bar'><p><strong>")
                .append(escape(user.getName())).append("</strong><br>Bendrabutis Nr. ")
                .append(user.getDormitoryId()).append(", ")
                .append(account.isAdmin() ? "administratorius" : "gyventojas")
                .append("</p><form method='post' action='/logout'>")
                .append("<button class='secondary'>Atsijungti</button></form></div>")
                .append(message);
        if (account.isAdmin()) {
            int duration = settings.findForDormitory(user.getDormitoryId()).durationMinutes();
            body.append("<section><h2>Rezervacijos trukmė</h2><p>Dabar: ")
                    .append(duration).append(" min.</p><form method='post' action='/admin/settings'>")
                    .append("<label>Nauja trukmė (min.) <input name='minutes' type='number' min='1' max='960' required></label>")
                    .append("<button>Išsaugoti</button></form></section>");
            body.append("<section><h2>Naujas gyventojas</h2>")
                    .append("<p>Paskyra kuriama tik šiame bendrabutyje.</p>")
                    .append("<form method='post' action='/admin/residents'>")
                    .append("<label>Vardas <input name='name' maxlength='100' required></label>")
                    .append("<label>Prisijungimo vardas <input name='login' pattern='[a-zA-Z0-9_]{3,30}' required></label>")
                    .append("<label>Slaptažodis <input name='password' type='password' minlength='8' required></label>")
                    .append("<button>Sukurti paskyrą</button></form></section>");
        } else {
            body.append("<section><h2>Laisvi laikai</h2><form action='/availability'>")
                    .append(machineSelect(user)).append(dateInput())
                    .append("<button>Rodyti</button></form></section>");
            body.append("<section><h2>Rezervuoti</h2><form method='post' action='/reservations'>")
                    .append(machineSelect(user))
                    .append(dateInput())
                    .append(timeSelect())
                    .append("<button>Rezervuoti</button></form></section>");
            body.append("<section><h2>Mano būsimos rezervacijos</h2>");
            List<Reservation> mine = service.getMyReservations(user);
            if (mine.isEmpty()) body.append("<p>Kol kas būsimų rezervacijų nėra.</p>");
            body.append("<div class='reservation-list'>");
            for (Reservation reservation : mine) {
                body.append("<div class='reservation'><div><strong>Mašina Nr. ")
                        .append(reservation.getWashingMachineId()).append("</strong><br>")
                        .append(reservation.getStartTime().format(DISPLAY_DATE_TIME))
                        .append("–").append(reservation.getEndTime().format(DISPLAY_TIME))
                        .append("<br><small>Rezervacija Nr. ").append(reservation.getId())
                        .append("</small></div><form method='post' action='/cancel'><input type='hidden' ")
                        .append("name='reservationId' value='").append(reservation.getId())
                        .append("'><button class='secondary'>Atšaukti</button></form></div>");
            }
            body.append("</div></section>");
        }
        body.append("<details class='api'><summary>Techninė informacija</summary>")
                .append("<p>JSON API: <a href='/api/reservations'>rezervacijos</a></p></details>");
        send(exchange, status, "text/html", page(body.toString()));
    }

    private static String dateInput() {
        return "<label>Data <input type='date' name='date' lang='lt-LT' min='"
                + LocalDate.now() + "' required></label>";
    }

    private static String timeSelect() {
        StringBuilder html = new StringBuilder("<fieldset><legend>Laikas (24 val.)</legend>"
                + "<div class='time-select'><label>Valanda <select name='hour' required>"
                + "<option value='' selected disabled>Pasirinkti</option>");
        for (int hour = 7; hour <= 22; hour++) {
            String value = twoDigits(hour);
            html.append("<option value='").append(value).append("'>").append(value).append("</option>");
        }
        html.append("</select></label><span aria-hidden='true'>:</span>"
                + "<label>Minutė <select name='minute' required>"
                + "<option value='' selected disabled>Pasirinkti</option>");
        for (int minute = 0; minute < 60; minute += ReservationService.START_STEP_MINUTES) {
            String value = twoDigits(minute);
            html.append("<option value='").append(value).append("'>").append(value).append("</option>");
        }
        return html.append("</select></label></div></fieldset>").toString();
    }

    private static String twoDigits(int number) {
        return number < 10 ? "0" + number : Integer.toString(number);
    }

    private String machineSelect(User user) {
        StringBuilder html = new StringBuilder("<label>Mašina <select name='machineId'>");
        for (WashingMachine machine : catalog.findMachines(user.getDormitoryId())) {
            html.append("<option value='").append(machine.getId()).append("'>Nr. ")
                    .append(machine.getId()).append("</option>");
        }
        return html.append("</select></label>").toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String page(String body) throws IOException {
        try (var input = WebServer.class.getResourceAsStream("/page.html")) {
            if (input == null) throw new IOException("Nerastas page.html.");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{body}}", body);
        }
    }

    private static String readForm(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> fields(String encoded) {
        Map<String, String> values = new HashMap<>();
        if (encoded == null || encoded.isBlank()) return values;
        for (String item : encoded.split("&")) {
            String[] pair = item.split("=", 2);
            values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.length == 2 ? pair[1] : "", StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String required(Map<String, String> fields, String name) {
        String value = fields.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trūksta lauko: " + name);
        }
        return value;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Įveskite datą metai-mėnuo-diena, pvz., 2030-11-22.");
        }
    }

    private static LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value.trim(), DISPLAY_TIME);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Įveskite laiką 24 valandų formatu, pvz., 15:00.");
        }
    }

    private static void send(HttpExchange exchange, int status, String type, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(303, -1);
    }

    private record Session(Account account, Instant expiresAt) { }
}
