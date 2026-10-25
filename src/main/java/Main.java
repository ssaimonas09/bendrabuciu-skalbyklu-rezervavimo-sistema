
public class Main {

    public static void main(String[] args) {

        Dormitory dormitoryA = new Dormitory(1, "Bendrabutis A");

        User studentA = new User(101, "Studentas A", 1);

        WashingMachine machine = new WashingMachine(2, 1);

        System.out.println("Bendrabučių skalbyklų rezervavimo sistema");

        System.out.println("Bendrabutis: " + dormitoryA.getName());

        System.out.println("Naudotojas: " + studentA.getName());

        System.out.println("Skalbimo mašina Nr.: " + machine.getId());


        ReservationRepository repository = new ReservationRepository();

        Reservation reservation = new Reservation(
                1,
                1,
                101,
                2,
                java.time.LocalDateTime.of(2026, 11, 5, 15, 0),
                java.time.LocalDateTime.of(2026, 11, 5, 16, 0)
        );

        repository.save(reservation);

        System.out.println("Bendrabučio A rezervacijų skaičius: "
                + repository.findByDormitoryId(1).size());


    }

}
