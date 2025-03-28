package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.Drone;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main {

    public static void main(String[] args) {

        // Initialize incident and zone files from resources.
        InputStream incidentFile = Main.class.getResourceAsStream("/incidentFile.csv");
        InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");
        String schedulerAddress = "localhost"; // Scheduler's IP
        int schedulerPort = 6002; // Scheduler's port

        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(incidentFile);
            parser.parseZoneFile(zoneFile);
        } catch (IOException e) {
            System.err.println("Failed to parse incidentFile.csv or zone_location.csv, aborting.");
            e.printStackTrace();
            return;
        }

        List<Zone> zones = parser.getZones();
        Map<Integer, Zone> zoneMap = parser.getZoneMap();


        // Initialize Scheduler and related
        Scheduler scheduler;
        try {
            scheduler = new Scheduler(zones, schedulerPort);
        } catch (IOException e) {
            System.err.println("Failed to create scheduler, aborting.");
            e.printStackTrace();
            return;
        }


        // Initialize Fire Incident Subsystem
        FireIncidentSubsystem fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);


        // Get user input for number of drones, or just modify numDrones
        Scanner scanner = new Scanner(System.in);
        int numDrones = 3;

        while (numDrones <= 0) {
            try {
                System.out.print("Enter the number of drones to initialize (positive integer): ");
                numDrones = Integer.parseInt(scanner.nextLine());
                if (numDrones <= 0) {
                    System.out.println("Please enter a valid positive integer.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }

        // Create and start drone threads
        List<Thread> droneThreads = new ArrayList<>();
        for (int i = 1; i <= numDrones; i++) {
            String droneName = "Drone" + i;
            Drone drone = new Drone(droneName, schedulerAddress, schedulerPort, zoneMap, "drone_faults.csv");
            Thread droneThread = new Thread(drone, droneName);
            droneThreads.add(droneThread);
        }

        // Create thread for Fire Incident Subsystem
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");

        // Start Fire Incident Subsystem
        FIsubsystemThread.start();

        // Start drone threads
        for (Thread droneThread : droneThreads) {
            droneThread.start();
        }
    }
}
