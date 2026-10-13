/** Links one patient with one doctor on a given date and time. */
public class Appointment {
    private final int id;
    private final int patientId;
    private final int doctorId;
    private final String date; // yyyy-MM-dd
    private final String time; // HH:mm

    public Appointment(int id, int patientId, int doctorId, String date, String time) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.date = date;
        this.time = time;
    }

    public int getId() { return id; }
    public int getPatientId() { return patientId; }
    public int getDoctorId() { return doctorId; }
    public String getDate() { return date; }
    public String getTime() { return time; }

    public String toCsv() {
        return id + "," + patientId + "," + doctorId + "," + date + "," + time;
    }

    public static Appointment fromCsv(String line) {
        String[] p = line.split(",", -1);
        return new Appointment(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                Integer.parseInt(p[2]), p[3], p[4]);
    }
}
