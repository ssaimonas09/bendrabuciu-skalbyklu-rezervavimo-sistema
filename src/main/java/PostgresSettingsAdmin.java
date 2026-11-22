import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PostgresSettingsAdmin {
    private final String url;
    private final String dbUser;
    private final String dbPassword;

    public PostgresSettingsAdmin(String url, String dbUser, String dbPassword) {
        this.url = url;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void changeDuration(int dormitoryId, int minutes) {
        if (minutes < 1 || minutes > 960) {
            throw new IllegalArgumentException("Trukme turi buti nuo 1 iki 960 minuciu.");
        }
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
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
