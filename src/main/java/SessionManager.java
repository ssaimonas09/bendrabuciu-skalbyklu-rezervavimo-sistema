import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    static final long SESSION_SECONDS = 8 * 60 * 60;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(Account account) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(account, Instant.now().plusSeconds(SESSION_SECONDS)));
        return token;
    }

    public Account find(String token) {
        if (token == null) return null;
        Session session = sessions.get(token);
        if (session == null) return null;
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(token);
            return null;
        }
        return session.account();
    }

    public void remove(String token) {
        if (token != null) sessions.remove(token);
    }

    private record Session(Account account, Instant expiresAt) { }
}
