import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class WebServer {
    private final PageRenderer pages = new PageRenderer();
    private final HttpAuthentication authentication;
    private final PostgresAccounts accounts;
    private final PostgresMachineCatalog machineCatalog;
    private final PostgresReservationSettingsRepository settings;
    private final ReservationService service;
    private final ReservationStore reservations;

    public WebServer(String url, String dbUser, String dbPassword) {
        accounts = new PostgresAccounts(url, dbUser, dbPassword);
        authentication = new HttpAuthentication(accounts);
        machineCatalog = new PostgresMachineCatalog(url, dbUser, dbPassword);
        settings = new PostgresReservationSettingsRepository(url, dbUser, dbPassword);
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
        server.createContext("/", this::handleRequest);
        server.start();
        return server;
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Account account = null;
        try {
            String method = exchange.getRequestMethod();
            if (method.equals("GET") && path.equals("/login")) {
                sendLoginPage(exchange);
                return;
            }
            if (method.equals("POST") && path.equals("/login")) {
                handleLogin(exchange);
                return;
            }
            if (method.equals("POST") && path.equals("/logout")) {
                handleLogout(exchange);
                return;
            }

            boolean apiRequest = path.startsWith("/api/");
            account = authentication.authenticateRequest(exchange, apiRequest);
            if (account == null) {
                if (apiRequest) {
                    send(exchange, 401, "text/plain", "Prisijunkite.");
                } else {
                    redirect(exchange, "/login");
                }
                return;
            }

            routeAuthenticatedRequest(exchange, account, method, path);
        } catch (IllegalArgumentException | DateTimeException exception) {
            sendInputError(exchange, account, path, exception);
        } catch (Exception exception) {
            exception.printStackTrace();
            send(exchange, 500, "text/plain", "Serverio klaida.");
        } finally {
            exchange.close();
        }
    }

    private void routeAuthenticatedRequest(HttpExchange exchange, Account account, String method, String path)
            throws IOException {
        switch (method + " " + path) {
            case "GET /" -> sendHomePage(exchange, account, "");
            case "GET /availability" -> sendAvailabilityPage(exchange, account);
            case "GET /api/reservations" -> sendReservationsJson(exchange, account);
            case "GET /api/availability" -> sendAvailabilityJson(exchange, account);
            case "POST /reservations" -> handleReservationCreation(exchange, account);
            case "POST /cancel" -> handleReservationCancellation(exchange, account);
            case "POST /admin/settings" -> handleDurationChange(exchange, account);
            case "POST /admin/residents" -> handleResidentRegistration(exchange, account);
            default -> send(exchange, 404, "text/plain", "Puslapis nerastas.");
        }
    }

    private void sendLoginPage(HttpExchange exchange) throws IOException {
        send(exchange, 200, "text/html", pages.renderLogin(""));
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!authentication.signIn(exchange)) {
            send(exchange, 401, "text/html", pages.renderLogin(
                    "<p>Neteisingas prisijungimo vardas arba slaptažodis.</p>"));
            return;
        }
        redirect(exchange, "/");
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        authentication.signOut(exchange);
        redirect(exchange, "/login");
    }

    private void sendAvailabilityPage(HttpExchange exchange, Account account) throws IOException {
        if (!requireResident(exchange, account)) return;
        RequestFields query = RequestFields.fromQuery(exchange);
        if (!requireOwnDormitory(exchange, account, query)) return;
        WashingMachine machine = findMachineInOwnDormitory(account, query.requireField("machineId"));
        LocalDate date = query.parseDate();
        List<LocalDateTime> times = service.getAvailableStartTimes(account.user(), machine, date);
        sendHomePage(exchange, account, pages.renderAvailability(machine, date, times));
    }

    private void handleReservationCreation(HttpExchange exchange, Account account) throws IOException {
        if (!requireResident(exchange, account)) return;
        RequestFields form = RequestFields.fromForm(exchange);
        if (!requireOwnDormitory(exchange, account, form)) return;
        WashingMachine machine = findMachineInOwnDormitory(account, form.requireField("machineId"));
        LocalDateTime start = form.parseReservationStart();
        Reservation created = service.createReservation(account.user(), machine, start);
        sendHomePage(exchange, account, pages.renderMessage("Sukurta rezervacija Nr. " + created.getId() + "."));
    }

    private void handleReservationCancellation(HttpExchange exchange, Account account) throws IOException {
        if (!requireResident(exchange, account)) return;
        RequestFields form = RequestFields.fromForm(exchange);
        if (!requireOwnDormitory(exchange, account, form)) return;
        service.cancelReservation(account.user(),
                Integer.parseInt(form.requireField("reservationId")));
        sendHomePage(exchange, account, pages.renderMessage("Rezervacija atšaukta."));
    }

    private void handleDurationChange(HttpExchange exchange, Account account) throws IOException {
        if (!requireAdmin(exchange, account)) return;
        RequestFields form = RequestFields.fromForm(exchange);
        if (!requireOwnDormitory(exchange, account, form)) return;
        settings.updateReservationDuration(account.user().getDormitoryId(),
                Integer.parseInt(form.requireField("minutes")));
        sendHomePage(exchange, account, pages.renderMessage("Trukmė pakeista."));
    }

    private void handleResidentRegistration(HttpExchange exchange, Account account) throws IOException {
        if (!requireAdmin(exchange, account)) return;
        RequestFields form = RequestFields.fromForm(exchange);
        if (!requireOwnDormitory(exchange, account, form)) return;
        String login = form.requireField("login").trim();
        accounts.createResidentAccount(account.user().getDormitoryId(),
                form.requireField("name"), login, form.requireField("password"));
        sendHomePage(exchange, account, pages.renderMessage("Gyventojo paskyra sukurta: " + login + "."));
    }

    private void sendReservationsJson(HttpExchange exchange, Account account) throws IOException {
        RequestFields query = RequestFields.fromQuery(exchange);
        if (!requireOwnDormitory(exchange, account, query)) return;
        List<Reservation> found = account.isAdmin()
                ? reservations.findByDormitoryId(account.user().getDormitoryId())
                : service.getUpcomingReservationsForUser(account.user());
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

    private void sendAvailabilityJson(HttpExchange exchange, Account account) throws IOException {
        if (!requireResident(exchange, account)) return;
        RequestFields query = RequestFields.fromQuery(exchange);
        if (!requireOwnDormitory(exchange, account, query)) return;
        WashingMachine machine = findMachineInOwnDormitory(account, query.requireField("machineId"));
        List<LocalDateTime> times = service.getAvailableStartTimes(account.user(),
                machine, query.parseDate());
        StringBuilder json = new StringBuilder("[");
        for (LocalDateTime time : times) {
            if (json.length() > 1) json.append(',');
            json.append('"').append(time).append('"');
        }
        send(exchange, 200, "application/json", json.append(']').toString());
    }

    private void sendHomePage(HttpExchange exchange, Account account, String message) throws IOException {
        sendHomePage(exchange, account, message, 200);
    }

    private void sendHomePage(HttpExchange exchange, Account account, String message, int status)
            throws IOException {
        User user = account.user();
        String html;
        if (account.isAdmin()) {
            int duration = settings.findForDormitory(user.getDormitoryId()).durationMinutes();
            html = pages.renderAdminHome(account, message, duration);
        } else {
            List<WashingMachine> machines = machineCatalog.findMachinesByDormitoryId(user.getDormitoryId());
            List<Reservation> upcomingReservations = service.getUpcomingReservationsForUser(user);
            html = pages.renderResidentHome(account, message, machines, upcomingReservations);
        }
        send(exchange, status, "text/html", html);
    }

    private boolean requireOwnDormitory(HttpExchange exchange, Account account,
                                     RequestFields fields) throws IOException {
        String requested = fields.getOptionalField("dormitoryId");
        if (requested != null && Integer.parseInt(requested) != account.user().getDormitoryId()) {
            send(exchange, 403, "text/plain", "Kito bendrabučio duomenys nepasiekiami.");
            return false;
        }
        return true;
    }

    private boolean requireResident(HttpExchange exchange, Account account) throws IOException {
        if (account.isAdmin()) {
            send(exchange, 403, "text/plain", "Veiksmas skirtas gyventojui.");
            return false;
        }
        return true;
    }

    private boolean requireAdmin(HttpExchange exchange, Account account) throws IOException {
        if (account.isAdmin()) return true;
        send(exchange, 403, "text/plain", "Reikia administratoriaus teisių.");
        return false;
    }

    private WashingMachine findMachineInOwnDormitory(Account account, String machineId) {
        int id = Integer.parseInt(machineId);
        for (WashingMachine machine : machineCatalog.findMachinesByDormitoryId(account.user().getDormitoryId())) {
            if (machine.getId() == id) {
                return machine;
            }
        }
        throw new IllegalArgumentException("Šiame bendrabutyje tokios mašinos nėra.");
    }

    private void sendInputError(HttpExchange exchange, Account account, String path,
                                Exception exception) throws IOException {
        String message = exception.getMessage() == null ? "Neteisingi duomenys."
                : exception.getMessage();
        if (path.startsWith("/api/")) {
            send(exchange, 400, "text/plain", message);
        } else {
            String notice = pages.renderError(message);
            if (account == null) {
                send(exchange, 400, "text/html", pages.renderLogin(notice));
            } else {
                sendHomePage(exchange, account, notice, 400);
            }
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

}
