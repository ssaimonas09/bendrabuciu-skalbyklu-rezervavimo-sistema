import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresAccounts {
    private final String url;
    private final String dbUser;
    private final String dbPassword;

    public PostgresAccounts(String url, String dbUser, String dbPassword) {
        this.url = url;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public Account authenticate(String login, String password) {
        String sql = "SELECT id, name, dormitory_id, role FROM users "
                + "WHERE login = ? AND password_hash = crypt(?, password_hash)";
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                User user = new User(result.getInt("id"), result.getString("name"),
                        result.getInt("dormitory_id"));
                return new Account(user, result.getString("role"));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Nepavyko patikrinti prisijungimo.", exception);
        }
    }

    public void createResident(int dormitoryId, String name, String login, String password) {
        name = name.trim();
        login = login.trim();
        if (name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Iveskite gyventojo varda (iki 100 simboliu).");
        }
        if (!login.matches("[a-zA-Z0-9_]{3,30}")) {
            throw new IllegalArgumentException("Prisijungimo vardas: 3-30 raidziu, skaiciu ar _.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Slaptazodis turi tureti bent 8 simbolius.");
        }

        String sql = "INSERT INTO users (dormitory_id, id, name, login, password_hash, role) "
                + "VALUES (?, nextval('user_id_seq'), ?, ?, crypt(?, gen_salt('bf')), "
                + "'RESIDENT')";
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, dormitoryId);
            statement.setString(2, name);
            statement.setString(3, login);
            statement.setString(4, password);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                throw new IllegalArgumentException("Prisijungimo vardas jau naudojamas.");
            }
            throw new IllegalStateException("Nepavyko sukurti gyventojo paskyros.", exception);
        }
    }
}
