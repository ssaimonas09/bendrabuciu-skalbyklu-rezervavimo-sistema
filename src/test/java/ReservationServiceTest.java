import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationServiceTest {

    @Test
    void createsReservationInOwnDormitory() {

        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User user = new User(101, "Studentas A", 1);
        WashingMachine machine = new WashingMachine(2, 1);

        service.createReservation(
                user,
                machine,
                LocalDateTime.of(2030, 11, 20, 15, 0)
        );

        assertEquals(1, repository.findByDormitoryId(1).size());
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

        assertEquals(0, repository.findByDormitoryId(1).size());
    }

    @Test
    void rejectsOverlappingReservation() {
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
    void allowsAdjacentReservations() {
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
    void rejectsReservationAfterClosingTime() {
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

        service.createReservation(
                user, machine,
                LocalDateTime.of(2030, 11, 20, 22, 0)
        );

        assertEquals(1, repository.findByDormitoryId(1).size());
    }

    private ReservationService createService(ReservationRepository repository) {
        Clock testClock = Clock.fixed(
                Instant.parse("2030-11-19T12:00:00Z"),
                ZoneOffset.UTC
        );

        return new ReservationService(repository, testClock);
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
    void studentACannotSeeDormitoryBReservations() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentB = new User(101, "Studentas B", 2);

        WashingMachine machineA = new WashingMachine(2, 1);
        WashingMachine machineB = new WashingMachine(2, 2);

        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation reservationA =
                service.createReservation(studentA, machineA, start);

        service.createReservation(studentB, machineB, start);

        assertEquals(1, service.getMyReservations(studentA).size());

        assertEquals(reservationA.getId(),
                service.getMyReservations(studentA).get(0).getId());
    }


    @Test
    void cannotCancelOtherDormitoryReservation() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = createService(repository);

        User studentA = new User(101, "Studentas A", 1);
        User studentB = new User(101, "Studentas B", 2);

        WashingMachine machineB = new WashingMachine(2, 2);

        Reservation reservationB = service.createReservation(
                studentB, machineB,
                LocalDateTime.of(2030, 11, 20, 15, 0)
        );

        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(studentA, reservationB.getId())
        );

        assertEquals(1, service.getMyReservations(studentB).size());
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

        service.cancelReservation(studentA, reservation.getId());

        assertEquals(0, service.getMyReservations(studentA).size());
    }



}
