import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReservationSettingsTest {
    private static final int DORMITORY_A_ID = 1;
    private static final int DORMITORY_B_ID = 2;
    private static final int STUDENT_ID = 101;
    private static final int MACHINE_ID = 2;

    private final Clock clock = Clock.fixed(
            Instant.parse("2030-11-19T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void appliesEachDormitoryReservationDuration() {
        ReservationSettingsSource settings = dormitoryId -> new ReservationSettings(
                dormitoryId == DORMITORY_A_ID ? 90 : 60, LocalTime.of(7, 0), LocalTime.of(23, 0));
        ReservationService service = new ReservationService(
                new InMemoryReservationRepository(), clock, settings);
        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationA = service.createReservation(
                new User(STUDENT_ID, "A", DORMITORY_A_ID), new WashingMachine(MACHINE_ID, DORMITORY_A_ID), start);
        Reservation reservationB = service.createReservation(
                new User(STUDENT_ID, "B", DORMITORY_B_ID), new WashingMachine(MACHINE_ID, DORMITORY_B_ID), start);

        assertEquals(LocalDateTime.of(2030, 11, 20, 16, 30), reservationA.getEndTime());
        assertEquals(LocalDateTime.of(2030, 11, 20, 16, 0), reservationB.getEndTime());
    }
}
