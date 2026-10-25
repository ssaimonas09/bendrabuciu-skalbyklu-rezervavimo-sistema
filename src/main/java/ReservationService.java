
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;


public class ReservationService {


    private final ReservationRepository repository;
    private final Clock clock;

    public ReservationService(ReservationRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public ReservationService(ReservationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }



    public Reservation createReservation(User user,
                                         WashingMachine machine,
                                         LocalDateTime startTime) {

        if (user.getDormitoryId() != machine.getDormitoryId()) {
            throw new IllegalArgumentException(
                    "Negalima rezervuoti kito bendrabučio mašinos."
            );
        }

        LocalDateTime endTime = startTime.plusHours(1);

        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            throw new IllegalArgumentException(
                    "Rezervacijos pradžia turi būti ateityje."
            );
        }



        LocalDateTime openingTime = startTime.toLocalDate().atTime(7, 0);
        LocalDateTime closingTime = startTime.toLocalDate().atTime(23, 0);

        if (startTime.isBefore(openingTime) || endTime.isAfter(closingTime)) {
            throw new IllegalArgumentException(
                    "Rezervacija turi tilpti į skalbyklos darbo laiką 07:00–23:00."
            );
        }


        if (hasMachineConflict(user.getDormitoryId(),
                machine.getId(), startTime, endTime)) {

            throw new IllegalArgumentException(
                    "Pasirinktas laikas jau užimtas."
            );
        }


        if (hasUserConflict(user.getDormitoryId(),
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

    private boolean hasMachineConflict(int dormitoryId, int machineId,
                                       LocalDateTime start, LocalDateTime end) {

        for (Reservation existing : repository.findByDormitoryId(dormitoryId)) {

            boolean sameMachine = existing.getWashingMachineId() == machineId;

            boolean overlaps = start.isBefore(existing.getEndTime())
                    && end.isAfter(existing.getStartTime());

            if (sameMachine && overlaps) {
                return true;
            }
        }

        return false;
    }


    private boolean hasUserConflict(int dormitoryId, int userId,
                                    LocalDateTime start, LocalDateTime end) {

        for (Reservation existing : repository.findByDormitoryId(dormitoryId)) {

            boolean sameUser = existing.getUserId() == userId;

            boolean overlaps = start.isBefore(existing.getEndTime())
                    && end.isAfter(existing.getStartTime());

            if (sameUser && overlaps) {
                return true;
            }
        }

        return false;
    }

    public List<Reservation> getMyReservations(User user) {

        List<Reservation> myReservations = new ArrayList<>();

        for (Reservation reservation :
                repository.findByDormitoryId(user.getDormitoryId())) {

            if (reservation.getUserId() == user.getId()) {
                myReservations.add(reservation);
            }
        }

        return myReservations;
    }


    public void cancelReservation(User user, int reservationId) {

        for (Reservation reservation :
                repository.findByDormitoryId(user.getDormitoryId())) {

            if (reservation.getId() == reservationId
                    && reservation.getUserId() == user.getId()) {

                repository.delete(reservation);
                return;
            }
        }

        throw new IllegalArgumentException(
                "Rezervacija nerasta arba neturite teisės jos atšaukti."
        );
    }



}
