/** Represents a doctor. */
public class Doctor {
    private final int id;
    private String name;
    private String specialization;
    private String phone;

    public Doctor(int id, String name, String specialization, String phone) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.phone = phone;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public String getPhone() { return phone; }

    public String toCsv() {
        return id + "," + name + "," + specialization + "," + phone;
    }

    public static Doctor fromCsv(String line) {
        String[] p = line.split(",", -1);
        return new Doctor(Integer.parseInt(p[0]), p[1], p[2], p[3]);
    }

    @Override
    public String toString() {
        return String.format("%-5d %-20s %-18s %s", id, name, specialization, phone);
    }
}
