import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReservationSettingsTest {
    private final Clock clock = Clock.fixed(
            Instant.parse("2030-11-19T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void changingOneDormitoryDurationDoesNotChangeTheOther() {
        ReservationSettingsSource settings = dormitoryId -> new ReservationSettings(
                dormitoryId == 1 ? 90 : 60, LocalTime.of(7, 0), LocalTime.of(23, 0));
        ReservationService service = new ReservationService(
                new ReservationRepository(), clock, settings);
        LocalDateTime start = LocalDateTime.of(2030, 11, 20, 15, 0);

        Reservation first = service.createReservation(
                new User(101, "A", 1), new WashingMachine(2, 1), start);
        Reservation second = service.createReservation(
                new User(101, "B", 2), new WashingMachine(2, 2), start);

        assertEquals(LocalDateTime.of(2030, 11, 20, 16, 30), first.getEndTime());
        assertEquals(LocalDateTime.of(2030, 11, 20, 16, 0), second.getEndTime());
    }

    @Test
    void longerReservationBlocksOverlappingTime() {
        ReservationService service = new ReservationService(
                new ReservationRepository(), clock,
                id -> new ReservationSettings(90, LocalTime.of(7, 0), LocalTime.of(23, 0)));
        WashingMachine machine = new WashingMachine(2, 1);
        service.createReservation(new User(101, "A", 1), machine,
                LocalDateTime.of(2030, 11, 20, 15, 0));

        assertThrows(IllegalArgumentException.class, () ->
                service.createReservation(new User(102, "B", 1), machine,
                        LocalDateTime.of(2030, 11, 20, 16, 0)));

        Reservation adjacent = service.createReservation(
                new User(102, "B", 1), machine, LocalDateTime.of(2030, 11, 20, 16, 30));
        assertEquals(LocalDateTime.of(2030, 11, 20, 18, 0), adjacent.getEndTime());
    }

    @Test
    void availableTimesUseEachDormitoryDuration() {
        ReservationSettingsSource settings = id -> new ReservationSettings(
                id == 1 ? 90 : 60, LocalTime.of(7, 0), LocalTime.of(23, 0));
        ReservationService service = new ReservationService(
                new ReservationRepository(), clock, settings);
        User a = new User(101, "A", 1);
        User b = new User(101, "B", 2);
        WashingMachine machineA = new WashingMachine(2, 1);
        WashingMachine machineB = new WashingMachine(2, 2);
        LocalDate date = LocalDate.of(2030, 11, 20);
        service.createReservation(a, machineA, date.atTime(15, 0));

        List<LocalDateTime> timesA = service.getAvailableStartTimes(
                new User(102, "Kitas", 1), machineA, date);
        List<LocalDateTime> timesB = service.getAvailableStartTimes(b, machineB, date);

        assertFalse(timesA.contains(date.atTime(14, 30)));
        assertFalse(timesA.contains(date.atTime(16, 0)));
        assertTrue(timesA.contains(date.atTime(16, 30)));
        assertFalse(timesA.contains(date.atTime(22, 0)));
        assertTrue(timesB.contains(date.atTime(15, 0)));
        assertTrue(timesB.contains(date.atTime(22, 0)));
    }

    @Test
    void availableTimesExcludeOtherMachineWhenUserIsBusy() {
        ReservationService service = new ReservationService(
                new ReservationRepository(), clock,
                id -> new ReservationSettings(60, LocalTime.of(7, 0), LocalTime.of(23, 0)));
        User student = new User(101, "A", 1);
        LocalDate date = LocalDate.of(2030, 11, 20);
        service.createReservation(student, new WashingMachine(2, 1), date.atTime(15, 0));

        List<LocalDateTime> available = service.getAvailableStartTimes(
                student, new WashingMachine(3, 1), date);

        assertFalse(available.contains(date.atTime(15, 0)));
        assertTrue(available.contains(date.atTime(16, 0)));
        assertThrows(IllegalArgumentException.class, () ->
                service.getAvailableStartTimes(student, new WashingMachine(2, 2), date));
    }

    @Test
    void listsOnlyFutureReservationsAndCannotCancelStartedOne() {
        ReservationRepository repository = new ReservationRepository();
        ReservationService service = new ReservationService(repository, clock,
                id -> new ReservationSettings(60, LocalTime.of(7, 0), LocalTime.of(23, 0)));
        User student = new User(101, "A", 1);
        LocalDate date = LocalDate.of(2030, 11, 19);
        Reservation started = new Reservation(repository.generateId(), 1, 101, 2,
                date.atTime(11, 30), date.atTime(12, 30));
        repository.save(started);
        Reservation future = service.createReservation(
                student, new WashingMachine(2, 1), date.atTime(15, 0));

        assertEquals(List.of(future.getId()),
                service.getMyReservations(student).stream().map(Reservation::getId).toList());
        assertThrows(IllegalArgumentException.class, () ->
                service.cancelReservation(student, started.getId()));
        assertEquals(2, repository.findByDormitoryId(1).size());
        service.cancelReservation(student, future.getId());
        assertEquals(1, repository.findByDormitoryId(1).size());
    }
}
