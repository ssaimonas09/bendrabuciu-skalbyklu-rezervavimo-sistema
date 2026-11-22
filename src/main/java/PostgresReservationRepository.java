import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostgresReservationRepository implements ReservationStore {
    private final String url;
    private final String username;
    private final String password;

    public PostgresReservationRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public int generateId() {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT nextval('reservation_id_seq')");
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko gauti rezervacijos numerio.", exception);
        }
    }

    @Override
    public void save(Reservation reservation) {
        String sql = "INSERT INTO reservations "
                + "(id, dormitory_id, user_id, washing_machine_id, start_time, end_time) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservation.getId());
            statement.setInt(2, reservation.getDormitoryId());
            statement.setInt(3, reservation.getUserId());
            statement.setInt(4, reservation.getWashingMachineId());
            statement.setTimestamp(5, Timestamp.valueOf(reservation.getStartTime()));
            statement.setTimestamp(6, Timestamp.valueOf(reservation.getEndTime()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko issaugoti rezervacijos.", exception);
        }
    }

    @Override
    public List<Reservation> findByDormitoryId(int dormitoryId) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT id, dormitory_id, user_id, washing_machine_id, start_time, end_time "
                + "FROM reservations WHERE dormitory_id = ? ORDER BY start_time, id";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dormitoryId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    reservations.add(new Reservation(
                            result.getInt("id"),
                            result.getInt("dormitory_id"),
                            result.getInt("user_id"),
                            result.getInt("washing_machine_id"),
                            result.getTimestamp("start_time").toLocalDateTime(),
                            result.getTimestamp("end_time").toLocalDateTime()
                    ));
                }
            }
            return reservations;
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko perskaityti rezervaciju.", exception);
        }
    }

    @Override
    public void delete(Reservation reservation) {
        String sql = "DELETE FROM reservations WHERE id = ? AND dormitory_id = ? AND user_id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservation.getId());
            statement.setInt(2, reservation.getDormitoryId());
            statement.setInt(3, reservation.getUserId());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko atsaukti rezervacijos.", exception);
        }
    }
}
