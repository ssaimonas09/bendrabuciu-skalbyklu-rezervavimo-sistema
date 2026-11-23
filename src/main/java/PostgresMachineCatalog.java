import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresMachineCatalog {
    private final String url;
    private final String username;
    private final String password;

    public PostgresMachineCatalog(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public List<WashingMachine> findMachinesByDormitoryId(int dormitoryId) {
        List<WashingMachine> machines = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, dormitory_id FROM washing_machines "
                             + "WHERE dormitory_id = ? ORDER BY id")) {
            statement.setInt(1, dormitoryId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    machines.add(new WashingMachine(result.getInt("id"),
                            result.getInt("dormitory_id")));
                }
            }
            return machines;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko perskaityti skalbimo masinu.", exception);
        }
    }
}
