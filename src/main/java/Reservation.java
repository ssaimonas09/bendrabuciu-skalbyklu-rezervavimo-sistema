
import java.time.LocalDateTime;

public class Reservation {

    private final int id;
    private final int dormitoryId;
    private final int userId;
    private final int washingMachineId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public Reservation(int id, int dormitoryId, int userId,
                       int washingMachineId, LocalDateTime startTime,
                       LocalDateTime endTime) {
        this.id = id;
        this.dormitoryId = dormitoryId;
        this.userId = userId;
        this.washingMachineId = washingMachineId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public int getDormitoryId() {
        return dormitoryId;
    }

    public int getUserId() {
        return userId;
    }

    public int getWashingMachineId() {
        return washingMachineId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
