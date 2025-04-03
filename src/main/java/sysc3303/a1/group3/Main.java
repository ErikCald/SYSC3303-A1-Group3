package sysc3303.a1.group3;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import sysc3303.a1.group3.drone.Drone;

public class Main {

    public static void main(String[] args) {

        // Load incident and zone files from resources.
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


        Scheduler scheduler;
        try {
            scheduler = new Scheduler(zones, schedulerPort);
        } catch (IOException e) {
            System.err.println("Failed to create scheduler, aborting.");
            e.printStackTrace();
            return;
        }

        // Create Fire Incident Subsystem
        FireIncidentSubsystem fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);

        // Get user input for number of drones, or just modify numDrones
        Scanner scanner = new Scanner(System.in);
        int numDrones = 12;

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
            String droneName = "drone" + i;
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
