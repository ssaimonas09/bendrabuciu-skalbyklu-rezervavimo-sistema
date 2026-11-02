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

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime startTime = LocalDateTime.of(2030, 11, 20, 15, 0);
        LocalDateTime expectedEndTime = LocalDateTime.of(2030, 11, 20, 16, 0);

        Reservation created = service.createReservation(user, machine, startTime);

        List<Reservation> storedReservations =
                repository.findByDormitoryId(user.getDormitoryId());

        assertEquals(1, storedReservations.size());
        Reservation stored = storedReservations.get(0);
        assertEquals(created.getId(), stored.getId());
        assertEquals(user.getId(), stored.getUserId());
        assertEquals(machine.getId(), stored.getWashingMachineId());
        assertEquals(user.getDormitoryId(), stored.getDormitoryId());
        assertEquals(startTime, stored.getStartTime());
        assertEquals(expectedEndTime, stored.getEndTime());
    }

    @Test
    void rejectsMachineFromOtherDormitory() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(5, 2);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        user,
                        machine,
                        LocalDateTime.of(2030, 11, 20, 15, 0)
                )
        );

        assertEquals(0, repository.findByDormitoryId(user.getDormitoryId()).size());
        assertEquals(0, repository.findByDormitoryId(machine.getDormitoryId()).size());
    }

    @Test
    void rejectsOverlappingReservationsForSameMachine() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentB = new User(102, "Studentas B", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentA, machine, start);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        studentB, machine, start.plusMinutes(30)
                )
        );

        assertEquals(1, repository.findByDormitoryId(1).size());
    }

    @Test
    void allowsAdjacentReservationsForSameMachine() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentB = new User(102, "Studentas B", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentA, machine, start);
        service.createReservation(studentB, machine, start.plusHours(1));

        assertEquals(2, repository.findByDormitoryId(1).size());
    }

    @Test
    void rejectsReservationBeforeOpeningTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        user, machine,
                        LocalDateTime.of(2030, 11, 20, 6, 30)
                )
        );

        assertEquals(0, repository.findByDormitoryId(1).size());
    }

    @Test
    void rejectsReservationEndingAfterClosingTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        user, machine,
                        LocalDateTime.of(2030, 11, 20, 22, 30)
                )
        );

        assertEquals(0, repository.findByDormitoryId(1).size());
    }

    @Test
    void allowsReservationEndingAtClosingTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        Reservation reservation = service.createReservation(
                user, machine,
                LocalDateTime.of(2030, 11, 20, 22, 0)
        );

        assertEquals(1, repository.findByDormitoryId(1).size());
        assertEquals(LocalDateTime.of(2030, 11, 20, 23, 0), reservation.getEndTime());
    }

    @Test
    void rejectsPastReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        user, machine,
                        LocalDateTime.of(2030, 11, 19, 11, 0)
                )
        );

        assertEquals(0, repository.findByDormitoryId(1).size());
    }

    @Test
    void rejectsReservationAtCurrentTime() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        user, machine,
                        LocalDateTime.of(2030, 11, 19, 12, 0)
                )
        );

        assertEquals(0, repository.findByDormitoryId(1).size());
    }

    @Test
    void rejectsOverlappingReservationsForSameUser() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User student = new User(101, "Studentas A", 1);

        WashingMachine machineA = new WashingMachine(2, 1);
        WashingMachine machineB = new WashingMachine(3, 1);

        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(student, machineA, start);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(
                        student, machineB, start.plusMinutes(30)
                )
        );

        assertEquals(1, repository.findByDormitoryId(1).size());
    }

    @Test
    void allowsAdjacentReservationsForSameUser() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User student = new User(101, "Studentas A", 1);

        WashingMachine machineA = new WashingMachine(2, 1);
        WashingMachine machineB = new WashingMachine(3, 1);

        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(student, machineA, start);
        service.createReservation(student, machineB, start.plusHours(1));

        assertEquals(2, repository.findByDormitoryId(1).size());
    }

    @Test
    void cannotSeeOtherDormitoryReservationsWithSameUserId() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentInDormitoryA = new User(101, "Studentas A", 1);
        User studentInDormitoryB = new User(101, "Studentas B", 2);

        WashingMachine machineInDormitoryA = new WashingMachine(2, 1);
        WashingMachine machineInDormitoryB = new WashingMachine(2, 2);

        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationA =
                service.createReservation(studentInDormitoryA, machineInDormitoryA, start);

        Reservation reservationB =
                service.createReservation(studentInDormitoryB, machineInDormitoryB, start);

        List<Reservation> reservationsA = service.getMyReservations(studentInDormitoryA);
        List<Reservation> reservationsB = service.getMyReservations(studentInDormitoryB);

        assertEquals(1, reservationsA.size());
        assertEquals(reservationA.getId(), reservationsA.get(0).getId());
        assertEquals(1, reservationsB.size());
        assertEquals(reservationB.getId(), reservationsB.get(0).getId());
    }

    @Test
    void cannotCancelOtherDormitoryReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentInDormitoryA = new User(101, "Studentas A", 1);
        User studentInDormitoryB = new User(101, "Studentas B", 2);

        WashingMachine machineInDormitoryB = new WashingMachine(2, 2);

        Reservation reservationB = service.createReservation(
                studentInDormitoryB, machineInDormitoryB,
                LocalDateTime.of(2030, 11, 20, 15, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(studentInDormitoryA, reservationB.getId())
        );

        List<Reservation> reservations = service.getMyReservations(studentInDormitoryB);

        assertEquals(1, reservations.size());
        assertEquals(reservationB.getId(), reservations.get(0).getId());
    }

    @Test
    void ownerCanCancelOwnReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        WashingMachine machineA = new WashingMachine(2, 1);

        Reservation reservation = service.createReservation(
                studentA, machineA,
                LocalDateTime.of(2030, 11, 20, 15, 0)
        );

        User otherStudent = new User(102, "Studentas C", 1);
        Reservation otherReservation = service.createReservation(
                otherStudent, machineA,
                LocalDateTime.of(2030, 11, 20, 16, 0)
        );

        service.cancelReservation(studentA, reservation.getId());

        assertEquals(0, service.getMyReservations(studentA).size());
        List<Reservation> remaining = repository.findByDormitoryId(studentA.getDormitoryId());
        assertEquals(1, remaining.size());
        assertEquals(otherReservation.getId(), remaining.get(0).getId());
    }

    @Test
    void cannotSeeOtherStudentsReservationsInSameDormitory() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentC = new User(102, "Studentas C", 1);

        WashingMachine machine2 = new WashingMachine(2, 1);
        WashingMachine machine3 = new WashingMachine(3, 1);

        LocalDateTime startTime =
                LocalDateTime.of(2030, 11, 20, 15, 0);

        service.createReservation(studentC, machine2, startTime);
        Reservation reservationA =
                service.createReservation(studentA, machine3, startTime);

        List<Reservation> reservations =
                service.getMyReservations(studentA);

        assertEquals(1, reservations.size());
        assertEquals(reservationA.getId(), reservations.get(0).getId());
    }

    @Test
    void cannotCancelOtherStudentsReservationInSameDormitory() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentC = new User(102, "Studentas C", 1);

        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime startTime =
                LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationC =
                service.createReservation(studentC, machine, startTime);

        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(studentA, reservationC.getId())
        );

        List<Reservation> reservations =
                service.getMyReservations(studentC);

        assertEquals(1, reservations.size());
        assertEquals(reservationC.getId(), reservations.get(0).getId());
    }

    @Test
    void allowsReservationStartingAtSeven() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User student = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime startTime =
                LocalDateTime.of(2030, 11, 20, 7, 0);

        Reservation reservation =
                service.createReservation(student, machine, startTime);

        assertEquals(startTime, reservation.getStartTime());
        assertEquals(
                LocalDateTime.of(2030, 11, 20, 8, 0),
                reservation.getEndTime()
        );
    }

    @Test
    void rejectsReservationOverlappingFromEarlierStart() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentC = new User(102, "Studentas C", 1);

        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime existingStart =
                LocalDateTime.of(2030, 11, 20, 15, 0);

        LocalDateTime newStart =
                LocalDateTime.of(2030, 11, 20, 14, 30);

        Reservation existing = service.createReservation(studentA, machine, existingStart);

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(studentC, machine, newStart)
        );

        List<Reservation> reservations = repository.findByDormitoryId(studentA.getDormitoryId());
        assertEquals(1, reservations.size());
        assertEquals(existing.getId(), reservations.get(0).getId());
    }

    @Test
    void allowsReservationAfterCancellation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User student = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        LocalDateTime startTime =
                LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation firstReservation =
                service.createReservation(student, machine, startTime);

        service.cancelReservation(student, firstReservation.getId());

        Reservation secondReservation =
                service.createReservation(student, machine, startTime);

        List<Reservation> reservations = service.getMyReservations(student);

        assertEquals(1, reservations.size());
        Reservation stored = reservations.get(0);
        assertEquals(secondReservation.getId(), stored.getId());
        assertNotEquals(firstReservation.getId(), stored.getId());
        assertEquals(machine.getId(), stored.getWashingMachineId());
        assertEquals(startTime, stored.getStartTime());
    }

    private ReservationService createService(ReservationRepository repository) {
        Clock testClock = Clock.fixed(
                Instant.parse("2030-11-19T12:00:00Z"),
                ZoneOffset.UTC
        );

        return new ReservationService(repository, testClock);
    }
}
