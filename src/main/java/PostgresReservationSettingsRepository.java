import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresReservationSettingsRepository implements ReservationSettingsSource {
    private final String url;
    private final String username;
    private final String password;

    public PostgresReservationSettingsRepository(String url, String username, String password) {
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

    public void updateReservationDuration(int dormitoryId, int minutes) {
        if (minutes < 1 || minutes > 960) {
            throw new IllegalArgumentException("Trukme turi buti nuo 1 iki 960 minuciu.");
        }
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dormitories SET reservation_minutes = ? WHERE id = ?")) {
            statement.setInt(1, minutes);
            statement.setInt(2, dormitoryId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Bendrabutis nerastas.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko pakeisti trukmes.", exception);
        }
    }
}
