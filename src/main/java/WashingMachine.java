
public class WashingMachine {

    private final int id;
    private final int dormitoryId;

    public WashingMachine(int id, int dormitoryId) {
        this.id = id;
        this.dormitoryId = dormitoryId;
    }

    public int getId() {
        return id;
    }

    public int getDormitoryId() {
        return dormitoryId;
    }
}
