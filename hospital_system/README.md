# Hospital Management System (Java)

A console application that manages patients, doctors and appointments.
It uses Object-Oriented Programming (classes, objects, methods, encapsulation) and
Java collections (`ArrayList`). Records are saved to text files in a `data/` folder,
so they are still there the next time you run it.

## Features
- Register patients, view all patients, search by name
- Add doctors and view the doctor list
- Schedule appointments (checks that the patient and doctor exist, the date is valid and
  not in the past, and the doctor is not already booked at that time)
- View appointments (sorted by date and time) and cancel them

## Requirements
JDK 17 or newer.

## Compile and run
```
javac *.java
java Main
```

## Files
| File | Purpose |
|------|---------|
| `Patient.java`, `Doctor.java`, `Appointment.java` | Data classes with private fields and getters |
| `HospitalService.java` | Stores records in collections, enforces booking rules, saves/loads files |
| `Main.java` | Menu and user input |

## Adding MySQL later
All storage goes through `HospitalService` (`save()` and `load()`), so a MySQL version
only needs to replace those two methods and the add/cancel methods with JDBC calls.
