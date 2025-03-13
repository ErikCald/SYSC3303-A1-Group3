package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.Drone;
import sysc3303.a1.group3.drone.DroneSpecifications;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Load incident and zone files from resources.
        InputStream incidentFile = Main.class.getResourceAsStream("/incidentFile.csv");
        InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");
        String schedulerAddress = "localhost"; // Scheduler's IP
        int schedulerPort = 5000; // Scheduler's port

        Scheduler scheduler;
        try {
            scheduler = new Scheduler(zoneFile);
        } catch (IOException e) {
            System.err.println("Failed to parse zone_location.csv, aborting.");
            e.printStackTrace();
            return;
        }

        FireIncidentSubsystem fiSubsystem;
        try {
            fiSubsystem = new FireIncidentSubsystem(scheduler, incidentFile, schedulerAddress, schedulerPort);
        } catch (IOException e) {
            System.err.println("Failed to parse incidentFile.csv, aborting.");
            e.printStackTrace();
            return;
        }

        // Create three drone instances.
        Drone drone1 = new Drone("drone1", scheduler, schedulerAddress, schedulerPort);
        Drone drone2 = new Drone("drone2", scheduler, schedulerAddress, schedulerPort);
        Drone drone3 = new Drone("drone3", scheduler, schedulerAddress, schedulerPort);

        scheduler.addDrone(drone1);
        scheduler.addDrone(drone2);
        scheduler.addDrone(drone3);
        scheduler.setSubsystem(fiSubsystem);

        // Create threads for the subsystem and each drone.
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread1 = new Thread(drone1, "Drone1");
        Thread DroneThread2 = new Thread(drone2, "Drone2");
        Thread DroneThread3 = new Thread(drone3, "Drone3");

        System.out.println("\nPress Enter to start the simulation:");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();

        // Start all threads.
        FIsubsystemThread.start();
        DroneThread1.start();
        DroneThread2.start();
        DroneThread3.start();
    }
}
