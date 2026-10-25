
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {

    private final List<Reservation> reservations = new ArrayList<>();

    private int nextId = 1;

    public int generateId() {
        return nextId++;
    }

    public void save(Reservation reservation) {
        reservations.add(reservation);
    }

    public List<Reservation> findByDormitoryId(int dormitoryId) {

        List<Reservation> result = new ArrayList<>();

        for (Reservation reservation : reservations) {
            if (reservation.getDormitoryId() == dormitoryId) {
                result.add(reservation);
            }
        }

        return result;
    }

    public void delete(Reservation reservation) {
        reservations.remove(reservation);
    }

}
