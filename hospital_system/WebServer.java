import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embedded HTTP server that provides REST API and serves the modern Web Dashboard
 * for the Hospital Management System, with full persistence to the data/ CSV files.
 */
public class WebServer {
    private static final int PREFERRED_PORT = 8080;
    private static final HospitalService hospital = new HospitalService("data");
    private static Path webDir;

    public static void main(String[] args) throws IOException {
        hospital.load();
        seedIfEmpty();

        // Determine web directory (relative to current directory or script directory)
        Path localWeb = Paths.get("web");
        if (Files.exists(localWeb)) {
            webDir = localWeb;
        } else {
            Path nestedWeb = Paths.get("hospital_system", "web");
            if (Files.exists(nestedWeb)) {
                webDir = nestedWeb;
            } else {
                webDir = localWeb;
            }
        }

        HttpServer server = createServer(PREFERRED_PORT);
        int port = server.getAddress().getPort();

        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/patients", new PatientsHandler());
        server.createContext("/api/doctors", new DoctorsHandler());
        server.createContext("/api/appointments", new AppointmentsHandler());
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        String url = "http://localhost:" + port;
        System.out.println("==================================================");
        System.out.println("PulseCare Hospital Management System - Web Server");
        System.out.println("Server running at: " + url);
        System.out.println("Press Ctrl+C to stop the server.");
        System.out.println("==================================================");

        // Try opening the browser
        openBrowser(url);
    }

    private static HttpServer createServer(int startingPort) {
        int port = startingPort;
        while (port < startingPort + 20) {
            try {
                return HttpServer.create(new InetSocketAddress(port), 0);
            } catch (IOException e) {
                port++;
            }
        }
        throw new RuntimeException("Could not find an available port to start the web server.");
    }

    private static void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {}

        try {
            // Windows fallback
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
        } catch (Exception ignored) {}
    }

    private static void seedIfEmpty() {
        if (hospital.getDoctors().isEmpty()) {
            hospital.addDoctor("Dr. Sarah Jenkins", "Cardiology", "+1 (555) 234-5678");
            hospital.addDoctor("Dr. Alexander Wright", "Neurology", "+1 (555) 345-6789");
            hospital.addDoctor("Dr. Priya Sharma", "Pediatrics", "+1 (555) 456-7890");
            hospital.addDoctor("Dr. Marcus Vance", "Orthopedics", "+1 (555) 567-8901");
            hospital.addDoctor("Dr. Elena Rostova", "General Medicine", "+1 (555) 678-9012");
        }

        if (hospital.getPatients().isEmpty()) {
            hospital.addPatient("Eleanor Vance", 34, "Female", "+1 (555) 101-2020", "Hypertension Consultation");
            hospital.addPatient("Liam Cooper", 42, "Male", "+1 (555) 202-3030", "Migraine & Neurological Screening");
            hospital.addPatient("Sophia Rodriguez", 8, "Female", "+1 (555) 303-4040", "Routine Pediatric Wellness Check");
            hospital.addPatient("Jackson Hayes", 29, "Male", "+1 (555) 404-5050", "Knee Ligament Strain (Sports Injury)");
            hospital.addPatient("Maya Patel", 51, "Female", "+1 (555) 505-6060", "Type 2 Diabetes Follow-up");
        }

        if (hospital.getAppointments().isEmpty() && !hospital.getPatients().isEmpty() && !hospital.getDoctors().isEmpty()) {
            LocalDate today = LocalDate.now();
            String d1 = today.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String d2 = today.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String d3 = today.plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE);

            hospital.scheduleAppointment(hospital.getPatients().get(0).getId(), hospital.getDoctors().get(0).getId(), d1, "09:30");
            hospital.scheduleAppointment(hospital.getPatients().get(1).getId(), hospital.getDoctors().get(1).getId(), d1, "11:00");
            hospital.scheduleAppointment(hospital.getPatients().get(2).getId(), hospital.getDoctors().get(2).getId(), d2, "14:15");
            hospital.scheduleAppointment(hospital.getPatients().get(3).getId(), hospital.getDoctors().get(3).getId(), d3, "10:00");
        }
    }

    // ---------- HTTP Handlers ----------

    private static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
                sendMethodNotAllowed(ex);
                return;
            }

            int totalPatients = hospital.getPatients().size();
            int totalDoctors = hospital.getDoctors().size();
            List<Appointment> apps = hospital.getAppointments();
            int totalAppointments = apps.size();

            String todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            long todayAppointments = apps.stream().filter(a -> a.getDate().equals(todayStr)).count();

            String json = String.format(
                "{\"totalPatients\":%d,\"totalDoctors\":%d,\"totalAppointments\":%d,\"todayAppointments\":%d}",
                totalPatients, totalDoctors, totalAppointments, todayAppointments
            );
            sendResponse(ex, 200, "application/json", json);
        }
    }

    private static class PatientsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            String method = ex.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                String query = ex.getRequestURI().getQuery();
                List<Patient> list;
                if (query != null && query.startsWith("search=")) {
                    String term = java.net.URLDecoder.decode(query.substring(7), StandardCharsets.UTF_8);
                    list = hospital.searchPatients(term);
                } else {
                    list = hospital.getPatients();
                }

                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Patient p = list.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format(
                        "{\"id\":%d,\"name\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"phone\":\"%s\",\"disease\":\"%s\"}",
                        p.getId(), esc(p.getName()), p.getAge(), esc(p.getGender()), esc(p.getPhone()), esc(p.getDisease())
                    ));
                }
                sb.append("]");
                sendResponse(ex, 200, "application/json", sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(ex.getRequestBody());
                Map<String, String> data = parseSimpleJson(body);

                String name = data.getOrDefault("name", "").trim();
                String ageStr = data.getOrDefault("age", "0").trim();
                String gender = data.getOrDefault("gender", "").trim();
                String phone = data.getOrDefault("phone", "").trim();
                String disease = data.getOrDefault("disease", "").trim();

                int age;
                try {
                    age = Integer.parseInt(ageStr);
                } catch (NumberFormatException e) {
                    sendResponse(ex, 400, "application/json", "{\"error\":\"Age must be a valid number.\"}");
                    return;
                }

                if (name.isEmpty() || gender.isEmpty() || phone.isEmpty() || disease.isEmpty()) {
                    sendResponse(ex, 400, "application/json", "{\"error\":\"All fields are required.\"}");
                    return;
                }

                Patient p = hospital.addPatient(name, age, gender, phone, disease);
                String json = String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"phone\":\"%s\",\"disease\":\"%s\"}",
                    p.getId(), esc(p.getName()), p.getAge(), esc(p.getGender()), esc(p.getPhone()), esc(p.getDisease())
                );
                sendResponse(ex, 201, "application/json", json);
            } else {
                sendMethodNotAllowed(ex);
            }
        }
    }

    private static class DoctorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            String method = ex.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Doctor> list = hospital.getDoctors();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Doctor d = list.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format(
                        "{\"id\":%d,\"name\":\"%s\",\"specialization\":\"%s\",\"phone\":\"%s\"}",
                        d.getId(), esc(d.getName()), esc(d.getSpecialization()), esc(d.getPhone())
                    ));
                }
                sb.append("]");
                sendResponse(ex, 200, "application/json", sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(ex.getRequestBody());
                Map<String, String> data = parseSimpleJson(body);

                String name = data.getOrDefault("name", "").trim();
                String spec = data.getOrDefault("specialization", "").trim();
                String phone = data.getOrDefault("phone", "").trim();

                if (name.isEmpty() || spec.isEmpty() || phone.isEmpty()) {
                    sendResponse(ex, 400, "application/json", "{\"error\":\"All fields are required.\"}");
                    return;
                }

                Doctor d = hospital.addDoctor(name, spec, phone);
                String json = String.format(
                    "{\"id\":%d,\"name\":\"%s\",\"specialization\":\"%s\",\"phone\":\"%s\"}",
                    d.getId(), esc(d.getName()), esc(d.getSpecialization()), esc(d.getPhone())
                );
                sendResponse(ex, 201, "application/json", json);
            } else {
                sendMethodNotAllowed(ex);
            }
        }
    }

    private static class AppointmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (handleCors(ex)) return;
            String method = ex.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Appointment> list = hospital.getAppointments();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Appointment a = list.get(i);
                    Patient p = hospital.findPatient(a.getPatientId());
                    Doctor d = hospital.findDoctor(a.getDoctorId());
                    if (i > 0) sb.append(",");
                    sb.append(String.format(
                        "{\"id\":%d,\"patientId\":%d,\"patientName\":\"%s\",\"doctorId\":%d,\"doctorName\":\"%s\",\"doctorSpecialization\":\"%s\",\"date\":\"%s\",\"time\":\"%s\"}",
                        a.getId(),
                        a.getPatientId(),
                        esc(p == null ? "Unknown Patient" : p.getName()),
                        a.getDoctorId(),
                        esc(d == null ? "Unknown Doctor" : d.getName()),
                        esc(d == null ? "General" : d.getSpecialization()),
                        esc(a.getDate()),
                        esc(a.getTime())
                    ));
                }
                sb.append("]");
                sendResponse(ex, 200, "application/json", sb.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(ex.getRequestBody());
                Map<String, String> data = parseSimpleJson(body);

                int patientId;
                int doctorId;
                try {
                    patientId = Integer.parseInt(data.getOrDefault("patientId", "0").trim());
                    doctorId = Integer.parseInt(data.getOrDefault("doctorId", "0").trim());
                } catch (NumberFormatException e) {
                    sendResponse(ex, 400, "application/json", "{\"error\":\"Invalid patient or doctor ID.\"}");
                    return;
                }

                String date = data.getOrDefault("date", "").trim();
                String time = data.getOrDefault("time", "").trim();

                String error = hospital.scheduleAppointment(patientId, doctorId, date, time);
                if (error != null) {
                    sendResponse(ex, 400, "application/json", "{\"error\":\"" + esc(error) + "\"}");
                } else {
                    sendResponse(ex, 201, "application/json", "{\"success\":true,\"message\":\"Appointment scheduled successfully.\"}");
                }

            } else if ("DELETE".equalsIgnoreCase(method)) {
                String query = ex.getRequestURI().getQuery();
                int id = -1;
                if (query != null && query.contains("id=")) {
                    for (String part : query.split("&")) {
                        if (part.startsWith("id=")) {
                            try {
                                id = Integer.parseInt(part.substring(3));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                if (id == -1) {
                    sendResponse(ex, 400, "application/json", "{\"error\":\"Missing appointment ID.\"}");
                    return;
                }

                boolean removed = hospital.cancelAppointment(id);
                if (removed) {
                    sendResponse(ex, 200, "application/json", "{\"success\":true,\"message\":\"Appointment cancelled.\"}");
                } else {
                    sendResponse(ex, 404, "application/json", "{\"error\":\"Appointment not found.\"}");
                }
            } else {
                sendMethodNotAllowed(ex);
            }
        }
    }

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Remove leading slash
            if (path.startsWith("/")) path = path.substring(1);

            Path target = webDir.resolve(path).normalize();
            if (!Files.exists(target) || Files.isDirectory(target)) {
                // Fallback to index.html for SPA if not found
                target = webDir.resolve("index.html");
            }

            if (!Files.exists(target)) {
                sendResponse(ex, 404, "text/plain", "404 Not Found");
                return;
            }

            String mime = getMimeType(target.getFileName().toString());
            byte[] bytes = Files.readAllBytes(target);
            ex.getResponseHeaders().set("Content-Type", mime);
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // ---------- Helper Methods ----------

    private static boolean handleCors(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void sendResponse(HttpExchange ex, int code, String contentType, String body) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendMethodNotAllowed(HttpExchange ex) throws IOException {
        sendResponse(ex, 405, "application/json", "{\"error\":\"Method not allowed\"}");
    }

    private static String readBody(InputStream is) throws IOException {
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    /** Simple JSON key-value parser for flat JSON structures */
    private static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+|true|false|null))");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1);
            String strVal = matcher.group(2);
            String rawVal = matcher.group(3);
            map.put(key, strVal != null ? strVal : (rawVal != null ? rawVal : ""));
        }
        return map;
    }

    private static String getMimeType(String filename) {
        if (filename.endsWith(".html")) return "text/html";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".ico")) return "image/x-icon";
        return "text/plain";
    }
}
