/** Represents a patient. Fields are private (encapsulation) and accessed through getters. */
public class Patient {
    private final int id;
    private String name;
    private int age;
    private String gender;
    private String phone;
    private String disease;

    public Patient(int id, String name, int age, String gender, String phone, String disease) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.disease = disease;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getDisease() { return disease; }

    /** One line for saving to the data file. */
    public String toCsv() {
        return id + "," + name + "," + age + "," + gender + "," + phone + "," + disease;
    }

    public static Patient fromCsv(String line) {
        String[] p = line.split(",", -1);
        return new Patient(Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2]), p[3], p[4], p[5]);
    }

    @Override
    public String toString() {
        return String.format("%-5d %-20s %-4d %-8s %-12s %s", id, name, age, gender, phone, disease);
    }
}
