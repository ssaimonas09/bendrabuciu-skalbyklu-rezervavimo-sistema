import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    static final int START_STEP_MINUTES = 30;
    private final ReservationStore repository;
    private final Clock clock;
    private final ReservationSettingsSource settingsSource;

    public ReservationService(ReservationStore repository, Clock clock,
                              ReservationSettingsSource settingsSource) {
        this.repository = repository;
        this.clock = clock;
        this.settingsSource = settingsSource;
    }

    public synchronized Reservation createReservation(User user,
                                                      WashingMachine machine,
                                                      LocalDateTime startTime) {
        if (user.getDormitoryId() != machine.getDormitoryId()) {
            throw new IllegalArgumentException(
                    "Negalima rezervuoti kito bendrabučio mašinos."
            );
        }

        ReservationSettings settings = settingsSource.findForDormitory(user.getDormitoryId());
        LocalDateTime endTime = startTime.plusMinutes(settings.durationMinutes());

        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException(
                    "Rezervacijos pradžia turi būti ateityje."
            );
        }

        if (startTime.getMinute() % START_STEP_MINUTES != 0
                || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new IllegalArgumentException(
                    "Pasirinkite laiką ties pilna arba puse valandos."
            );
        }

        LocalDateTime openingTime = startTime.toLocalDate().atTime(settings.opensAt());
        LocalDateTime closingTime = startTime.toLocalDate().atTime(settings.closesAt());

        if (startTime.isBefore(openingTime) || endTime.isAfter(closingTime)) {
            throw new IllegalArgumentException(
                    "Rezervacija turi tilpti i skalbyklos darbo laika."
            );
        }

        List<Reservation> reservations = repository.findByDormitoryId(user.getDormitoryId());
        if (hasMachineConflict(reservations,
                machine.getId(), startTime, endTime)) {
            throw new IllegalArgumentException(
                    "Pasirinktas laikas jau užimtas."
            );
        }

        if (hasUserConflict(reservations,
                user.getId(), startTime, endTime)) {
            throw new IllegalArgumentException(
                    "Studentas jau turi rezervaciją šiuo laiku."
            );
        }

        Reservation reservation = new Reservation(
                repository.generateId(),
                user.getDormitoryId(),
                user.getId(),
                machine.getId(),
                startTime,
                endTime
        );

        repository.save(reservation);

        return reservation;
    }

    private boolean hasMachineConflict(List<Reservation> reservations, int machineId,
                                       LocalDateTime start, LocalDateTime end) {
        for (Reservation existing : reservations) {
            boolean sameMachine = existing.getWashingMachineId() == machineId;

            boolean overlaps = overlapsExistingReservation(start, end, existing);

            if (sameMachine && overlaps) {
                return true;
            }
        }

        return false;
    }

    private boolean hasUserConflict(List<Reservation> reservations, int userId,
                                    LocalDateTime start, LocalDateTime end) {
        for (Reservation existing : reservations) {
            boolean sameUser = existing.getUserId() == userId;

            boolean overlaps = overlapsExistingReservation(start, end, existing);

            if (sameUser && overlaps) {
                return true;
            }
        }

        return false;
    }

    private boolean overlapsExistingReservation(LocalDateTime start, LocalDateTime end,
                                                Reservation existing) {
        return start.isBefore(existing.getEndTime())
                && end.isAfter(existing.getStartTime());
    }

    public List<LocalDateTime> getAvailableStartTimes(User user,
                                                       WashingMachine machine,
                                                       LocalDate date) {
        if (user.getDormitoryId() != machine.getDormitoryId()) {
            throw new IllegalArgumentException("Kito bendrabucio masina nepasiekiama.");
        }

        ReservationSettings settings = settingsSource.findForDormitory(user.getDormitoryId());
        List<Reservation> reservations = repository.findByDormitoryId(user.getDormitoryId());
        List<LocalDateTime> available = new ArrayList<>();
        LocalDateTime closing = date.atTime(settings.closesAt());
        LocalDateTime now = LocalDateTime.now(clock);

        for (LocalDateTime start = date.atTime(settings.opensAt());
             !start.plusMinutes(settings.durationMinutes()).isAfter(closing);
             start = start.plusMinutes(START_STEP_MINUTES)) {
            if (!start.isAfter(now)) {
                continue;
            }

            LocalDateTime end = start.plusMinutes(settings.durationMinutes());
            boolean hasConflict = hasMachineConflict(reservations, machine.getId(), start, end)
                    || hasUserConflict(reservations, user.getId(), start, end);
            if (!hasConflict) {
                available.add(start);
            }
        }
        return available;
    }

    public List<Reservation> getUpcomingReservationsForUser(User user) {
        List<Reservation> upcomingReservations = new ArrayList<>();

        for (Reservation reservation :
                repository.findByDormitoryId(user.getDormitoryId())) {
            if (reservation.getUserId() == user.getId()
                    && reservation.getStartTime().isAfter(LocalDateTime.now(clock))) {
                upcomingReservations.add(reservation);
            }
        }

        return upcomingReservations;
    }

    public void cancelReservation(User user, int reservationId) {
        for (Reservation reservation :
                repository.findByDormitoryId(user.getDormitoryId())) {
            if (reservation.getId() == reservationId
                    && reservation.getUserId() == user.getId()) {
                if (!reservation.getStartTime().isAfter(LocalDateTime.now(clock))) {
                    throw new IllegalArgumentException(
                            "Prasidejusios rezervacijos atsaukti negalima.");
                }
                repository.delete(reservation);
                return;
            }
        }

        throw new IllegalArgumentException(
                "Rezervacija nerasta arba neturite teisės jos atšaukti."
        );
    }
}
