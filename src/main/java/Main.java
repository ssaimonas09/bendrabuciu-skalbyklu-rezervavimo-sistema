
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.List;
import java.time.format.ResolverStyle;

public class Main {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        ReservationRepository repository = new ReservationRepository();
        ReservationService service = new ReservationService(repository);


        User studentA = new User(101, "Studentas A", 1);
        User studentB = new User(101, "Studentas B", 2);

        User student = studentA;

        List<WashingMachine> machines = List.of(
                new WashingMachine(2, 1),
                new WashingMachine(3, 1),
                new WashingMachine(2, 2),
                new WashingMachine(3, 2)
        );



        DateTimeFormatter format =
                DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
                        .withResolverStyle(ResolverStyle.STRICT);


        while (true) {

            printMenu(student);
            String choice = scanner.nextLine();

            if (choice.equals("0")) {
                break;
            }

            if (choice.equals("4")) {

                if (student.getDormitoryId() == 1) {
                    student = studentB;
                } else {
                    student = studentA;
                }

                System.out.println("Pasirinktas studentas: " + student.getName());
                continue;
            }



            if (choice.equals("2")) {
                showMyReservations(student, service, format);
                continue;
            }

            if (choice.equals("3")) {
                cancelMyReservation(scanner, service, student);
                continue;
            }

            if (!choice.equals("1")) {
                System.out.println("Neteisingas pasirinkimas.");
                continue;
            }


            createMyReservation(scanner, service, student, machines, format);
        }

        scanner.close();
    }

    private static void printMenu(User student) {
        System.out.println("\nDabartinis studentas: " + student.getName());
        System.out.println("Bendrabutis Nr.: " + student.getDormitoryId());

        System.out.println("\n1 - Sukurti rezervaciją");
        System.out.println("2 - Mano rezervacijos");
        System.out.println("3 - Atšaukti rezervaciją");
        System.out.println("4 - Pakeisti demonstracinį studentą");
        System.out.println("0 - Baigti programą");
        System.out.print("Pasirinkimas: ");
    }


    private static void showMyReservations(
            User student,
            ReservationService service,
            DateTimeFormatter format) {

        List<Reservation> myReservations =
                service.getMyReservations(student);

        if (myReservations.isEmpty()) {
            System.out.println("Rezervacijų nėra.");
        } else {
            for (Reservation reservation : myReservations) {
                System.out.println(
                        "Rezervacija Nr. " + reservation.getId()
                                + " | Mašina Nr. " + reservation.getWashingMachineId()
                                + " | Pradžia: " + reservation.getStartTime().format(format)
                                + " | Pabaiga: " + reservation.getEndTime().format(format)
                );
            }
        }
    }


    private static void cancelMyReservation(
            Scanner scanner,
            ReservationService service,
            User student) {

        try {
            System.out.print("Rezervacijos numeris: ");
            int reservationId = Integer.parseInt(scanner.nextLine());

            service.cancelReservation(student, reservationId);

            System.out.println("Rezervacija atšaukta.");

        } catch (NumberFormatException e) {
            System.out.println("Įveskite teisingą rezervacijos numerį.");

        } catch (IllegalArgumentException e) {
            System.out.println("Atšaukimas nepavyko: " + e.getMessage());
        }
    }


            private static void createMyReservation(
                    Scanner scanner,
                    ReservationService service,
                    User student,
                    List<WashingMachine> machines,
                    DateTimeFormatter format) {

                try {
                    System.out.print("Mašinos numeris (2 arba 3): ");
                    int machineId = Integer.parseInt(scanner.nextLine());

                    WashingMachine machine = null;

                    for (WashingMachine availableMachine : machines) {

                        if (availableMachine.getId() == machineId
                                && availableMachine.getDormitoryId() == student.getDormitoryId()) {

                            machine = availableMachine;
                            break;
                        }
                    }

                    if (machine == null) {
                        System.out.println("Tokios mašinos šiame bendrabutyje nėra.");
                        return;
                    }

                    System.out.print("Data ir laikas (yyyy-MM-dd HH:mm): ");

                    LocalDateTime startTime =
                            LocalDateTime.parse(scanner.nextLine(), format);

                    Reservation reservation =
                            service.createReservation(student, machine, startTime);

                    System.out.println("Rezervacija sėkmingai sukurta!");
                    System.out.println("Rezervacijos Nr.: " + reservation.getId());
                    System.out.println("Pradžia: " +
                            reservation.getStartTime().format(format));
                    System.out.println("Pabaiga: " +
                            reservation.getEndTime().format(format));

                } catch (DateTimeParseException | NumberFormatException e) {
                    System.out.println("Neteisingas duomenų formatas.");

                } catch (IllegalArgumentException e) {
                    System.out.println("Rezervacija atmesta: " + e.getMessage());
                }
            }




}



