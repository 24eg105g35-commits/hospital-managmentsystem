import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Holds all hospital data in Java collections (ArrayList) and saves it to
 * simple text files so records are still there next time the program runs.
 */
public class HospitalService {
    private final List<Patient> patients = new ArrayList<>();
    private final List<Doctor> doctors = new ArrayList<>();
    private final List<Appointment> appointments = new ArrayList<>();
    private final Path dir;

    public HospitalService(String dataFolder) {
        this.dir = Paths.get(dataFolder);
    }

    // ---------- Patients ----------
    public Patient addPatient(String name, int age, String gender, String phone, String disease) {
        Patient p = new Patient(nextPatientId(), clean(name), age, clean(gender), clean(phone), clean(disease));
        patients.add(p);
        save();
        return p;
    }

    public Patient findPatient(int id) {
        for (Patient p : patients) if (p.getId() == id) return p;
        return null;
    }

    public List<Patient> searchPatients(String text) {
        List<Patient> result = new ArrayList<>();
        for (Patient p : patients)
            if (p.getName().toLowerCase().contains(text.toLowerCase())) result.add(p);
        return result;
    }

    public List<Patient> getPatients() { return patients; }

    // ---------- Doctors ----------
    public Doctor addDoctor(String name, String specialization, String phone) {
        Doctor d = new Doctor(nextDoctorId(), clean(name), clean(specialization), clean(phone));
        doctors.add(d);
        save();
        return d;
    }

    public Doctor findDoctor(int id) {
        for (Doctor d : doctors) if (d.getId() == id) return d;
        return null;
    }

    public List<Doctor> getDoctors() { return doctors; }

    // ---------- Appointments ----------
    /** Returns null on success, or a message explaining why it could not be booked. */
    public String scheduleAppointment(int patientId, int doctorId, String date, String time) {
        if (findPatient(patientId) == null) return "No patient with ID " + patientId + ".";
        if (findDoctor(doctorId) == null) return "No doctor with ID " + doctorId + ".";
        try {
            if (LocalDate.parse(date).isBefore(LocalDate.now())) return "The date is in the past.";
            LocalTime.parse(time);
        } catch (DateTimeParseException e) {
            return "Invalid date or time. Use yyyy-MM-dd and HH:mm (for example 2027-01-15 and 10:30).";
        }
        for (Appointment a : appointments)
            if (a.getDoctorId() == doctorId && a.getDate().equals(date) && a.getTime().equals(time))
                return "That doctor is already booked at that date and time.";
        appointments.add(new Appointment(nextAppointmentId(), patientId, doctorId, date, time));
        save();
        return null;
    }

    public boolean cancelAppointment(int id) {
        boolean removed = appointments.removeIf(a -> a.getId() == id);
        if (removed) save();
        return removed;
    }

    public List<Appointment> getAppointments() {
        List<Appointment> sorted = new ArrayList<>(appointments);
        sorted.sort(Comparator.comparing(Appointment::getDate).thenComparing(Appointment::getTime));
        return sorted;
    }

    /** Readable one-line description of an appointment. */
    public String describe(Appointment a) {
        Patient p = findPatient(a.getPatientId());
        Doctor d = findDoctor(a.getDoctorId());
        return String.format("%-5d %-12s %-6s %-20s %s", a.getId(), a.getDate(), a.getTime(),
                p == null ? "(deleted)" : p.getName(), d == null ? "(deleted)" : "Dr. " + d.getName());
    }

    // ---------- Saving and loading ----------
    public void save() {
        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve("patients.csv"), patients.stream().map(Patient::toCsv).toList());
            Files.write(dir.resolve("doctors.csv"), doctors.stream().map(Doctor::toCsv).toList());
            Files.write(dir.resolve("appointments.csv"), appointments.stream().map(Appointment::toCsv).toList());
        } catch (IOException e) {
            System.out.println("Warning: could not save data (" + e.getMessage() + ")");
        }
    }

    public void load() {
        try {
            for (String l : readLines("patients.csv")) patients.add(Patient.fromCsv(l));
            for (String l : readLines("doctors.csv")) doctors.add(Doctor.fromCsv(l));
            for (String l : readLines("appointments.csv")) appointments.add(Appointment.fromCsv(l));
        } catch (IOException | RuntimeException e) {
            System.out.println("Warning: could not read saved data (" + e.getMessage() + ")");
        }
    }

    private List<String> readLines(String file) throws IOException {
        Path p = dir.resolve(file);
        return Files.exists(p) ? Files.readAllLines(p) : List.of();
    }

    // ---------- Helpers ----------
    private int nextPatientId() {
        int max = 0;
        for (Patient p : patients) max = Math.max(max, p.getId());
        return max + 1;
    }

    private int nextDoctorId() {
        int max = 0;
        for (Doctor d : doctors) max = Math.max(max, d.getId());
        return max + 1;
    }

    private int nextAppointmentId() {
        int max = 0;
        for (Appointment a : appointments) max = Math.max(max, a.getId());
        return max + 1;
    }

    /** Commas and line breaks would break the CSV files, so replace them. */
    private String clean(String s) {
        return s.replace(',', ' ').replace('\n', ' ').trim();
    }
}
