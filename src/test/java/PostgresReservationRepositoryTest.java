import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class PostgresReservationRepositoryTest {
    @Test
    void savedReservationSurvivesNewRepositoryInstance() {
        String url = System.getenv("JDBC_URL");
        assumeTrue(url != null && !url.isBlank(), "PostgreSQL reikalingas tik integraciniam testui");

        PostgresReservationRepository first = new PostgresReservationRepository(
                url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        Reservation reservation = new Reservation(
                first.generateId(), 1, 101, 2,
                LocalDateTime.of(2099, 1, 1, 15, 0),
                LocalDateTime.of(2099, 1, 1, 16, 0));

        try {
            first.save(reservation);
            PostgresReservationRepository second = new PostgresReservationRepository(
                    url, System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

            Reservation readBack = second.findByDormitoryId(1).stream()
                    .filter(item -> item.getId() == reservation.getId())
                    .findFirst().orElseThrow();
            assertEquals(reservation.getStartTime(), readBack.getStartTime());
            assertEquals(reservation.getEndTime(), readBack.getEndTime());
            assertFalse(second.findByDormitoryId(2).stream()
                    .anyMatch(item -> item.getId() == reservation.getId()));
        } finally {
            first.delete(reservation);
        }

        assertTrue(first.findByDormitoryId(1).stream()
                .noneMatch(item -> item.getId() == reservation.getId()));
    }
}
