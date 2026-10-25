
public class User {

    private final int id;
    private final String name;
    private final int dormitoryId;

    public User(int id, String name, int dormitoryId) {
        this.id = id;
        this.name = name;
        this.dormitoryId = dormitoryId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDormitoryId() {
        return dormitoryId;
    }
}
