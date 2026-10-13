import java.util.List;
import java.util.Scanner;

/** Console menu for the Hospital Management System. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final HospitalService hospital = new HospitalService("data");

    public static void main(String[] args) {
        hospital.load();
        System.out.println("=== Hospital Management System ===");
        while (true) {
            printMenu();
            int choice = readInt("Choose an option: ");
            System.out.println();
            switch (choice) {
                case 1 -> registerPatient();
                case 2 -> viewPatients();
                case 3 -> searchPatient();
                case 4 -> addDoctor();
                case 5 -> viewDoctors();
                case 6 -> scheduleAppointment();
                case 7 -> viewAppointments();
                case 8 -> cancelAppointment();
                case 0 -> {
                    hospital.save();
                    System.out.println("Data saved. Goodbye!");
                    return;
                }
                default -> System.out.println("Please choose a number from the menu.");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n1. Register patient      5. View doctors");
        System.out.println("2. View all patients     6. Schedule appointment");
        System.out.println("3. Search patient        7. View appointments");
        System.out.println("4. Add doctor            8. Cancel appointment");
        System.out.println("0. Save and exit");
    }

    private static void registerPatient() {
        String name = readText("Name: ");
        int age = readInt("Age: ");
        String gender = readText("Gender: ");
        String phone = readText("Phone: ");
        String disease = readText("Disease / reason for visit: ");
        Patient p = hospital.addPatient(name, age, gender, phone, disease);
        System.out.println("Patient registered with ID " + p.getId() + ".");
    }

    private static void viewPatients() {
        List<Patient> list = hospital.getPatients();
        if (list.isEmpty()) { System.out.println("No patients registered yet."); return; }
        System.out.printf("%-5s %-20s %-4s %-8s %-12s %s%n", "ID", "Name", "Age", "Gender", "Phone", "Disease");
        list.forEach(System.out::println);
    }

    private static void searchPatient() {
        String text = readText("Enter name (or part of it): ");
        List<Patient> found = hospital.searchPatients(text);
        if (found.isEmpty()) System.out.println("No matching patients.");
        else found.forEach(System.out::println);
    }

    private static void addDoctor() {
        String name = readText("Doctor name: ");
        String spec = readText("Specialization: ");
        String phone = readText("Phone: ");
        Doctor d = hospital.addDoctor(name, spec, phone);
        System.out.println("Doctor added with ID " + d.getId() + ".");
    }

    private static void viewDoctors() {
        List<Doctor> list = hospital.getDoctors();
        if (list.isEmpty()) { System.out.println("No doctors added yet."); return; }
        System.out.printf("%-5s %-20s %-18s %s%n", "ID", "Name", "Specialization", "Phone");
        list.forEach(System.out::println);
    }

    private static void scheduleAppointment() {
        int pid = readInt("Patient ID: ");
        int did = readInt("Doctor ID: ");
        String date = readText("Date (yyyy-MM-dd): ");
        String time = readText("Time (HH:mm): ");
        String error = hospital.scheduleAppointment(pid, did, date, time);
        System.out.println(error == null ? "Appointment scheduled." : "Could not schedule: " + error);
    }

    private static void viewAppointments() {
        List<Appointment> list = hospital.getAppointments();
        if (list.isEmpty()) { System.out.println("No appointments scheduled."); return; }
        System.out.printf("%-5s %-12s %-6s %-20s %s%n", "ID", "Date", "Time", "Patient", "Doctor");
        for (Appointment a : list) System.out.println(hospital.describe(a));
    }

    private static void cancelAppointment() {
        int id = readInt("Appointment ID to cancel: ");
        System.out.println(hospital.cancelAppointment(id) ? "Appointment cancelled." : "No such appointment.");
    }

    // ---------- Input helpers ----------
    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) { hospital.save(); System.exit(0); } // input ended
            String s = in.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("This field cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            String s = readText(prompt);
            try {
                int n = Integer.parseInt(s);
                if (n >= 0) return n;
            } catch (NumberFormatException ignored) { }
            System.out.println("Please enter a valid whole number.");
        }
    }
}
