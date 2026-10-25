
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WashingMachineTest {

    @Test
    void washingMachineHasCorrectDormitory() {

        WashingMachine machine = new WashingMachine(2, 1);

        int dormitoryId = machine.getDormitoryId();

        assertEquals(1, dormitoryId);
    }
}
