import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class HttpAuthentication {
    private static final String SESSION_COOKIE = "laundry_session";

    private final PostgresAccounts accounts;
    private final SessionManager sessions = new SessionManager();

    HttpAuthentication(PostgresAccounts accounts) {
        this.accounts = accounts;
    }

    boolean signIn(HttpExchange exchange) throws IOException {
        RequestFields form = RequestFields.fromForm(exchange);
        Account account = accounts.authenticate(form.requireField("login"), form.requireField("password"));
        if (account == null) {
            return false;
        }

        sessions.remove(readSessionToken(exchange));
        String token = sessions.create(account);
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE + "=" + token
                + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + SessionManager.SESSION_SECONDS);
        return true;
    }

    void signOut(HttpExchange exchange) {
        sessions.remove(readSessionToken(exchange));
        exchange.getResponseHeaders().set("Set-Cookie", SESSION_COOKIE
                + "=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
    }

    Account authenticateRequest(HttpExchange exchange, boolean allowBasic) {
        Account account = sessions.find(readSessionToken(exchange));
        if (account == null && allowBasic) {
            return authenticateBasic(exchange);
        }
        return account;
    }

    private static String readSessionToken(HttpExchange exchange) {
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
}
