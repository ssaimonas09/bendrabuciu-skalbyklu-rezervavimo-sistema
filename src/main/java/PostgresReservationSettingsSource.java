import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresReservationSettingsSource implements ReservationSettingsSource {
    private final String url;
    private final String username;
    private final String password;

    public PostgresReservationSettingsSource(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public ReservationSettings findForDormitory(int dormitoryId) {
        String sql = "SELECT reservation_minutes, opens_at, closes_at "
                + "FROM dormitories WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dormitoryId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("Bendrabutis nerastas.");
                }
                return new ReservationSettings(
                        result.getInt("reservation_minutes"),
                        result.getTime("opens_at").toLocalTime(),
                        result.getTime("closes_at").toLocalTime());
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko perskaityti bendrabucio nustatymu.", exception);
        }
    }
}
