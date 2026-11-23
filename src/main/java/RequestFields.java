import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

final class RequestFields {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Map<String, String> values = new HashMap<>();

    private RequestFields(String encoded) {
        if (encoded == null || encoded.isBlank()) return;
        for (String item : encoded.split("&")) {
            String[] pair = item.split("=", 2);
            values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.length == 2 ? pair[1] : "", StandardCharsets.UTF_8));
        }
    }

    static RequestFields fromForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new RequestFields(body);
    }

    static RequestFields fromQuery(HttpExchange exchange) {
        return new RequestFields(exchange.getRequestURI().getRawQuery());
    }

    String getOptionalField(String name) {
        return values.get(name);
    }

    String requireField(String name) {
        String value = getOptionalField(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Trūksta lauko: " + name);
        }
        return value;
    }

    LocalDate parseDate() {
        String value = requireField("date");
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Įveskite datą metai-mėnuo-diena, pvz., 2030-11-22.");
        }
    }

    LocalDateTime parseReservationStart() {
        LocalDate date = parseDate();
        String time = values.containsKey("time") ? requireField("time")
                : requireField("hour") + ":" + requireField("minute");
        try {
            return date.atTime(LocalTime.parse(time.trim(), TIME_FORMAT));
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("Įveskite laiką 24 valandų formatu, pvz., 15:00.");
        }
    }
}
