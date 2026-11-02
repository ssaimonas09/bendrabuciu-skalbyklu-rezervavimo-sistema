import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationServiceTest {
    @Test
    void createsReservationInOwnDormitory() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);
        LocalDateTime expectedEndTime = LocalDateTime.of(2030, 11, 20, 16, 0);

        Reservation reservation = service.createReservation(student, machine, startTime);

        List<Reservation> storedReservations = repository.findByDormitoryId(dormitoryId);

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
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryAId = 1;
        int dormitoryBId = 2;
        int studentId = 101;
        int machineId = 5;

        User student = new User(studentId, "Student A", dormitoryAId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryBId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, startTime));

        assertEquals(0, repository.findByDormitoryId(dormitoryAId).size());
        assertEquals(0, repository.findByDormitoryId(dormitoryBId).size());
    }

    @Test
    void rejectsOverlappingReservationsForSameMachine() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentAId = 101;
        int studentBId = 102;
        int machineId = 2;

        User studentA = new User(studentAId, "Student A", dormitoryId);
        User studentB = new User(studentBId, "Student B", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentA, machine, startTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(studentB, machine, startTime.plusMinutes(30)));

        assertEquals(1, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void allowsAdjacentReservationsForSameMachine() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentAId = 101;
        int studentBId = 102;
        int machineId = 2;

        User studentA = new User(studentAId, "Student A", dormitoryId);
        User studentB = new User(studentBId, "Student B", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentA, machine, startTime);
        service.createReservation(studentB, machine, startTime.plusHours(1));

        assertEquals(2, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void rejectsReservationBeforeOpeningTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 6, 30);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, startTime));

        assertEquals(0, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void rejectsReservationEndingAfterClosingTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 22, 30);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, startTime));

        assertEquals(0, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void allowsReservationEndingAtClosingTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 22, 0);
        LocalDateTime expectedEndTime = LocalDateTime.of(2030, 11, 20, 23, 0);

        Reservation reservation = service.createReservation(student, machine, startTime);

        assertEquals(1, repository.findByDormitoryId(dormitoryId).size());
        assertEquals(expectedEndTime, reservation.getEndTime());
    }

    @Test
    void rejectsPastReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 19, 11, 0);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, startTime));

        assertEquals(0, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void rejectsReservationAtCurrentTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 19, 12, 0);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machine, startTime));

        assertEquals(0, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void rejectsOverlappingReservationsForSameUser() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineAId = 2;
        int machineBId = 3;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machineA = new WashingMachine(machineAId, dormitoryId);
        WashingMachine machineB = new WashingMachine(machineBId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(student, machineA, startTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(student, machineB, startTime.plusMinutes(30)));

        assertEquals(1, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void allowsAdjacentReservationsForSameUser() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineAId = 2;
        int machineBId = 3;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machineA = new WashingMachine(machineAId, dormitoryId);
        WashingMachine machineB = new WashingMachine(machineBId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(student, machineA, startTime);
        service.createReservation(student, machineB, startTime.plusHours(1));

        assertEquals(2, repository.findByDormitoryId(dormitoryId).size());
    }

    @Test
    void cannotSeeOtherDormitoryReservationsWithSameUserId() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryAId = 1;
        int dormitoryBId = 2;
        int sharedStudentId = 101;
        int sharedMachineId = 2;

        User studentA = new User(sharedStudentId, "Student A", dormitoryAId);
        User studentB = new User(sharedStudentId, "Student B", dormitoryBId);
        WashingMachine machineA = new WashingMachine(sharedMachineId, dormitoryAId);
        WashingMachine machineB = new WashingMachine(sharedMachineId, dormitoryBId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationA = service.createReservation(studentA, machineA, startTime);
        Reservation reservationB = service.createReservation(studentB, machineB, startTime);

        List<Reservation> reservationsA = service.getMyReservations(studentA);
        List<Reservation> reservationsB = service.getMyReservations(studentB);

        assertEquals(1, reservationsA.size());
        assertEquals(reservationA.getId(), reservationsA.get(0).getId());
        assertEquals(1, reservationsB.size());
        assertEquals(reservationB.getId(), reservationsB.get(0).getId());
    }

    @Test
    void cannotCancelOtherDormitoryReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryAId = 1;
        int dormitoryBId = 2;
        int sharedStudentId = 101;
        int machineBId = 2;

        User studentA = new User(sharedStudentId, "Student A", dormitoryAId);
        User studentB = new User(sharedStudentId, "Student B", dormitoryBId);
        WashingMachine machineB = new WashingMachine(machineBId, dormitoryBId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);
        Reservation reservationB = service.createReservation(studentB, machineB, startTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(studentA, reservationB.getId()));

        List<Reservation> reservationsB = service.getMyReservations(studentB);

        assertEquals(1, reservationsB.size());
        assertEquals(reservationB.getId(), reservationsB.get(0).getId());
    }

    @Test
    void ownerCanCancelOwnReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentAId = 101;
        int studentBId = 102;
        int machineId = 2;

        User studentA = new User(studentAId, "Student A", dormitoryId);
        User studentB = new User(studentBId, "Student B", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationA = service.createReservation(studentA, machine, startTime);
        Reservation reservationB = service.createReservation(studentB, machine, startTime.plusHours(1));

        service.cancelReservation(studentA, reservationA.getId());

        List<Reservation> storedReservations = repository.findByDormitoryId(dormitoryId);

        assertEquals(0, service.getMyReservations(studentA).size());
        assertEquals(1, storedReservations.size());
        assertEquals(reservationB.getId(), storedReservations.get(0).getId());
    }

    @Test
    void cannotSeeOtherStudentsReservationsInSameDormitory() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentAId = 101;
        int studentBId = 102;
        int machineAId = 2;
        int machineBId = 3;

        User studentA = new User(studentAId, "Student A", dormitoryId);
        User studentB = new User(studentBId, "Student B", dormitoryId);
        WashingMachine machineA = new WashingMachine(machineAId, dormitoryId);
        WashingMachine machineB = new WashingMachine(machineBId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentB, machineA, startTime);
        Reservation reservationA = service.createReservation(studentA, machineB, startTime);

        List<Reservation> reservationsA = service.getMyReservations(studentA);

        assertEquals(1, reservationsA.size());
        assertEquals(reservationA.getId(), reservationsA.get(0).getId());
    }

    @Test
    void cannotCancelOtherStudentsReservationInSameDormitory() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentAId = 101;
        int studentBId = 102;
        int machineId = 2;

        User studentA = new User(studentAId, "Student A", dormitoryId);
        User studentB = new User(studentBId, "Student B", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationB = service.createReservation(studentB, machine, startTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(studentA, reservationB.getId()));

        List<Reservation> reservationsB = service.getMyReservations(studentB);

        assertEquals(1, reservationsB.size());
        assertEquals(reservationB.getId(), reservationsB.get(0).getId());
    }

    @Test
    void allowsReservationStartingAtOpeningTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 7, 0);
        LocalDateTime expectedEndTime = LocalDateTime.of(2030, 11, 20, 8, 0);

        Reservation reservation = service.createReservation(student, machine, startTime);

        assertEquals(startTime, reservation.getStartTime());
        assertEquals(expectedEndTime, reservation.getEndTime());
    }

    @Test
    void rejectsReservationOverlappingFromEarlierStart() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentAId = 101;
        int studentBId = 102;
        int machineId = 2;

        User studentA = new User(studentAId, "Student A", dormitoryId);
        User studentB = new User(studentBId, "Student B", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime existingStartTime = LocalDateTime.of(2030, 11, 20, 15, 0);
        LocalDateTime overlappingStartTime = LocalDateTime.of(2030, 11, 20, 14, 30);

        Reservation existingReservation = service.createReservation(studentA, machine, existingStartTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(studentB, machine, overlappingStartTime));

        List<Reservation> storedReservations = repository.findByDormitoryId(dormitoryId);

        assertEquals(1, storedReservations.size());
        assertEquals(existingReservation.getId(), storedReservations.get(0).getId());
    }

    @Test
    void allowsReservationAfterCancellation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        int dormitoryId = 1;
        int studentId = 101;
        int machineId = 2;

        User student = new User(studentId, "Student A", dormitoryId);
        WashingMachine machine = new WashingMachine(machineId, dormitoryId);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation cancelledReservation = service.createReservation(student, machine, startTime);

        service.cancelReservation(student, cancelledReservation.getId());

        Reservation replacementReservation = service.createReservation(student, machine, startTime);

        List<Reservation> reservations = service.getMyReservations(student);

        assertEquals(1, reservations.size());
        Reservation storedReservation = reservations.get(0);
        assertEquals(replacementReservation.getId(), storedReservation.getId());
        assertNotEquals(cancelledReservation.getId(), storedReservation.getId());
        assertEquals(machine.getId(), storedReservation.getWashingMachineId());
        assertEquals(startTime, storedReservation.getStartTime());
    }

    private ReservationService createService(ReservationRepository repository) {
        Clock testClock = Clock.fixed(
                Instant.parse("2030-11-19T12:00:00Z"),
                ZoneOffset.UTC
        );

        return new ReservationService(repository, testClock);
    }
}
