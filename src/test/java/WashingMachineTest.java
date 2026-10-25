
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WashingMachineTest {

    @Test
    void washingMachineHasCorrectDormitory() {

        // Arrange – paruošiame duomenis
        WashingMachine machine = new WashingMachine(2, 1);

        // Act – gauname bendrabučio numerį
        int dormitoryId = machine.getDormitoryId();

        // Assert – patikriname rezultatą
        assertEquals(1, dormitoryId);
    }
}
