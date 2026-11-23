import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationServiceTest {
    private static final int DORMITORY_A_ID = 1;
    private static final int DORMITORY_B_ID = 2;
    private static final int STUDENT_ID = 101;
    private static final int OTHER_STUDENT_ID = 102;
    private static final int MACHINE_ID = 2;
    private static final int OTHER_MACHINE_ID = 3;

    private final InMemoryReservationRepository repository = new InMemoryReservationRepository();
    private final ReservationService service = createServiceWithFixedClock();

    @Test
    void createsReservationInOwnDormitory() {
        User student = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        WashingMachine machine = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);
        LocalDateTime expectedEndTime = LocalDateTime.of(2030, 11, 20, 16, 0);

        Reservation reservation = service.createReservation(student, machine, startTime);

        List<Reservation> storedReservations = repository.findByDormitoryId(DORMITORY_A_ID);

        assertEquals(1, storedReservations.size());
        Reservation storedReservation = storedReservations.get(0);
        assertEquals(reservation.getId(), storedReservation.getId());
        assertEquals(student.getId(), storedReservation.getUserId());
        assertEquals(machine.getId(), storedReservation.getWashingMachineId());
        assertEquals(student.getDormitoryId(), storedReservation.getDormitoryId());
        assertEquals(startTime, storedReservation.getStartTime());
        assertEquals(expectedEndTime, storedReservation.getEndTime());
    }

    @Test
    void rejectsMachineFromOtherDormitory() {
        User student = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        int otherDormitoryMachineId = 5;
        WashingMachine machine = new WashingMachine(otherDormitoryMachineId, DORMITORY_B_ID);
        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, start));

        assertEquals("Negalima rezervuoti kito bendrabučio mašinos.", error.getMessage());
        assertEquals(0, repository.findByDormitoryId(DORMITORY_A_ID).size());
        assertEquals(0, repository.findByDormitoryId(DORMITORY_B_ID).size());
    }

    @Test
    void rejectsMachineOverlapAndAllowsAdjacentReservation() {
        User studentA = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        User studentB = new User(OTHER_STUDENT_ID, "Student B", DORMITORY_A_ID);
        WashingMachine machine = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentA, machine, startTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(studentB, machine, startTime.plusMinutes(30)));

        assertEquals(1, repository.findByDormitoryId(DORMITORY_A_ID).size());
        Reservation adjacent = service.createReservation(studentB, machine, startTime.plusHours(1));
        assertEquals(startTime.plusHours(2), adjacent.getEndTime());
    }

    @Test
    void cannotReadOrCancelOtherDormitoryReservation() {
        User studentA = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        User studentB = new User(STUDENT_ID, "Student B", DORMITORY_B_ID);
        WashingMachine machineA = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);
        WashingMachine machineB = new WashingMachine(MACHINE_ID, DORMITORY_B_ID);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationA = service.createReservation(studentA, machineA, startTime);
        Reservation reservationB = service.createReservation(studentB, machineB, startTime);

        List<Reservation> reservationsA = service.getUpcomingReservationsForUser(studentA);
        assertEquals(1, reservationsA.size());
        assertEquals(reservationA.getId(), reservationsA.get(0).getId());
        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(studentA, reservationB.getId()));

        List<Reservation> reservationsB = service.getUpcomingReservationsForUser(studentB);

        assertEquals(1, reservationsB.size());
        assertEquals(reservationB.getId(), reservationsB.get(0).getId());
    }

    @Test
    void rejectsPastStartTime() {
        User student = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        WashingMachine machine = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);
        LocalDateTime yesterday = LocalDateTime.of(2030, 11, 18, 15, 0);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, yesterday));
        assertEquals(0, repository.findByDormitoryId(DORMITORY_A_ID).size());
    }

    @Test
    void rejectsReservationEndingAfterClosingTime() {
        User student = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        WashingMachine machine = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);
        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 22, 30);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, start));
        assertEquals(0, repository.findByDormitoryId(DORMITORY_A_ID).size());
    }

    @Test
    void rejectsOverlappingReservationsForSameStudent() {
        User student = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        WashingMachine firstMachine = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);
        WashingMachine secondMachine = new WashingMachine(OTHER_MACHINE_ID, DORMITORY_A_ID);
        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(student, firstMachine, start);
        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, secondMachine, start.plusMinutes(30)));
        assertEquals(1, repository.findByDormitoryId(DORMITORY_A_ID).size());
    }

    @Test
    void allowsReservationAtOpeningTime() {
        User student = new User(STUDENT_ID, "Student A", DORMITORY_A_ID);
        WashingMachine machine = new WashingMachine(MACHINE_ID, DORMITORY_A_ID);
        LocalDateTime openingTime = LocalDateTime.of(2030, 11, 20, 7, 0);

        Reservation reservation = service.createReservation(student, machine, openingTime);

        assertEquals(openingTime, reservation.getStartTime());
        assertEquals(openingTime.plusHours(1), reservation.getEndTime());
    }

    private ReservationService createServiceWithFixedClock() {
        Clock testClock = Clock.fixed(
                Instant.parse("2030-11-19T12:00:00Z"),
                ZoneOffset.UTC
        );

        return new ReservationService(repository, testClock,
                id -> new ReservationSettings(60,
                        LocalTime.of(7, 0), LocalTime.of(23, 0)));
    }
}
