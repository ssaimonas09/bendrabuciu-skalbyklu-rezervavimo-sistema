import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PostgresReservationRepositoryTest {
    private static final int DORMITORY_A_ID = 1;
    private static final int DORMITORY_B_ID = 2;
    private static final int STUDENT_ID = 101;
    private static final int MACHINE_ID = 2;

    @Test
    void readsSavedReservationFromAnotherRepositoryInstance() {
        String url = System.getenv("JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL reikalingas tik integraciniam testui");

        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");
        PostgresReservationRepository writer = new PostgresReservationRepository(
                url, dbUser, dbPassword);
        Reservation reservation = new Reservation(
                writer.generateId(), DORMITORY_A_ID, STUDENT_ID, MACHINE_ID,
                LocalDateTime.of(2099, 1, 1, 15, 0),
                LocalDateTime.of(2099, 1, 1, 16, 0));

        try {
            writer.save(reservation);
            PostgresReservationRepository reader = new PostgresReservationRepository(
                    url, dbUser, dbPassword);

            Reservation readBack = findReservation(reader, DORMITORY_A_ID, reservation.getId());
            assertNotNull(readBack);
            assertEquals(reservation.getStartTime(), readBack.getStartTime());
            assertEquals(reservation.getEndTime(), readBack.getEndTime());
            assertNull(findReservation(reader, DORMITORY_B_ID, reservation.getId()));
        } finally {
            writer.delete(reservation);
        }

        assertNull(findReservation(writer, DORMITORY_A_ID, reservation.getId()));
    }

    private Reservation findReservation(PostgresReservationRepository repository, int dormitoryId, int id) {
        for (Reservation reservation : repository.findByDormitoryId(dormitoryId)) {
            if (reservation.getId() == id) {
                return reservation;
            }
        }
        return null;
    }
}
