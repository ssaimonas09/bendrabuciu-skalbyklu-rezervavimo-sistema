import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PageRenderer {
    private static final DateTimeFormatter DISPLAY_DATE_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("HH:mm");

    public String renderLogin(String message) throws IOException {
        String body = "<section><h2>Prisijungimas</h2>" + message
                + "<form method='post' action='/login'>"
                + "<label>Prisijungimo vardas <input name='login' autocomplete='username' required autofocus></label>"
                + "<label>Slaptažodis <input name='password' type='password' autocomplete='current-password' required></label>"
                + "<button>Prisijungti</button></form></section>";
        return wrapInPageTemplate(body);
    }

    public String renderAvailability(WashingMachine machine, LocalDate date,
                                     List<LocalDateTime> times) {
        StringBuilder result = new StringBuilder("<section><h2>Laisvi laikai</h2><p>");
        result.append("Mašina Nr. ").append(machine.getId()).append(", ")
                .append(date).append("</p><ul class='times'>");
        for (LocalDateTime time : times) {
            result.append("<li>").append(time.format(DISPLAY_TIME)).append("</li>");
        }
        if (times.isEmpty()) result.append("<li>Laisvų laikų nėra.</li>");
        result.append("</ul></section>");
        return result.toString();
    }

    public String renderAdminHome(Account account, String message, int duration) throws IOException {
        return renderHome(account, message, renderAdminForms(duration));
    }

    public String renderResidentHome(Account account, String message,
                                     List<WashingMachine> machines,
                                     List<Reservation> reservations) throws IOException {
        return renderHome(account, message,
                renderResidentForms(machines) + renderReservations(reservations));
    }

    private String renderHome(Account account, String message, String content) throws IOException {
        return wrapInPageTemplate(renderAccountBar(account) + message + content + renderApiLinks());
    }

    private String renderAccountBar(Account account) {
        User user = account.user();
        StringBuilder body = new StringBuilder("<div class='account-bar'><p><strong>")
                .append(escapeHtml(user.getName())).append("</strong><br>Bendrabutis Nr. ")
                .append(user.getDormitoryId()).append(", ")
                .append(account.isAdmin() ? "administratorius" : "gyventojas")
                .append("</p><form method='post' action='/logout'>")
                .append("<button class='secondary'>Atsijungti</button></form></div>");
        return body.toString();
    }

    private String renderAdminForms(int duration) {
        StringBuilder body = new StringBuilder();
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
        return body.toString();
    }

    private String renderResidentForms(List<WashingMachine> machines) {
        StringBuilder body = new StringBuilder();
        body.append("<section><h2>Laisvi laikai</h2><form action='/availability'>")
                .append(renderMachineSelect(machines)).append(renderDateInput())
                .append("<button>Rodyti</button></form></section>");
        body.append("<section><h2>Rezervuoti</h2><form method='post' action='/reservations'>")
                .append(renderMachineSelect(machines))
                .append(renderDateInput())
                .append(renderStartTimeSelect())
                .append("<button>Rezervuoti</button></form></section>");
        return body.toString();
    }

    private String renderReservations(List<Reservation> reservations) {
        StringBuilder body = new StringBuilder();
        body.append("<section><h2>Mano būsimos rezervacijos</h2>");
        if (reservations.isEmpty()) body.append("<p>Kol kas būsimų rezervacijų nėra.</p>");
        body.append("<div class='reservation-list'>");
        for (Reservation reservation : reservations) {
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
        return body.toString();
    }

    private String renderApiLinks() {
        StringBuilder body = new StringBuilder();
        body.append("<details class='api'><summary>Techninė informacija</summary>")
                .append("<p>JSON API: <a href='/api/reservations'>rezervacijos</a></p></details>");
        return body.toString();
    }

    public String renderMessage(String text) {
        return "<p class='message'>" + escapeHtml(text) + "</p>";
    }

    public String renderError(String text) {
        return "<p class='error' role='alert'>" + escapeHtml(text)
                + " <a href='/'>Grįžti į pradžią</a></p>";
    }

    private static String renderDateInput() {
        return "<label>Data <input type='date' name='date' lang='lt-LT' min='"
                + LocalDate.now() + "' required></label>";
    }

    private static String renderStartTimeSelect() {
        StringBuilder html = new StringBuilder("<fieldset><legend>Laikas (24 val.)</legend>"
                + "<div class='time-select'><label>Valanda <select name='hour' required>"
                + "<option value='' selected disabled>Pasirinkti</option>");
        for (int hour = 7; hour <= 22; hour++) {
            String value = formatTwoDigits(hour);
            html.append("<option value='").append(value).append("'>").append(value).append("</option>");
        }
        html.append("</select></label><span aria-hidden='true'>:</span>"
                + "<label>Minutė <select name='minute' required>"
                + "<option value='' selected disabled>Pasirinkti</option>");
        for (int minute = 0; minute < 60; minute += ReservationService.START_STEP_MINUTES) {
            String value = formatTwoDigits(minute);
            html.append("<option value='").append(value).append("'>").append(value).append("</option>");
        }
        return html.append("</select></label></div></fieldset>").toString();
    }

    private static String formatTwoDigits(int number) {
        return number < 10 ? "0" + number : Integer.toString(number);
    }

    private String renderMachineSelect(List<WashingMachine> machines) {
        StringBuilder html = new StringBuilder("<label>Mašina <select name='machineId'>");
        for (WashingMachine machine : machines) {
            html.append("<option value='").append(machine.getId()).append("'>Nr. ")
                    .append(machine.getId()).append("</option>");
        }
        return html.append("</select></label>").toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String wrapInPageTemplate(String body) throws IOException {
        try (var input = PageRenderer.class.getResourceAsStream("/page.html")) {
            if (input == null) throw new IOException("Nerastas page.html.");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("{{body}}", body);
        }
    }

}
