import java.util.List;

public interface ReservationStore {
    int generateId();

    void save(Reservation reservation);

    List<Reservation> findByDormitoryId(int dormitoryId);

    void delete(Reservation reservation);
}
