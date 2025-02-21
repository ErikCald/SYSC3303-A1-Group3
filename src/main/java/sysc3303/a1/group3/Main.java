package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.Drone;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // fileStream for scheduler, parses events in constructor
        InputStream incidentFile = Main.class.getResourceAsStream("/incidentFile.csv");
        InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");

        //Create required objects for simulation
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
            fiSubsystem = new FireIncidentSubsystem(scheduler, incidentFile);
        } catch (IOException e) {
            System.err.println("Failed to parse incidentFile.csv, aborting.");
            e.printStackTrace();
            return;
        }


        Drone drone1 = new Drone("drone1", scheduler);
        Drone drone2 = new Drone("drone2", scheduler);
        Drone drone3 = new Drone("drone3", scheduler);

        //Ensure scheduler aggregation is complete
        scheduler.addDrone(drone1);
        scheduler.addDrone(drone2);
        scheduler.addDrone(drone3);
        scheduler.setSubsystem(fiSubsystem);

        //Make threads from the aforementioned objects
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread1 = new Thread((drone1), "Drone1");
        Thread DroneThread2 = new Thread((drone2), "Drone2");
        Thread DroneThread3 = new Thread((drone3), "Drone3");

        // Wait for user to press Enter before starting
        System.out.println("\nPress Enter to start the simulation:");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();

        // Start the simulation
        FIsubsystemThread.start();
        DroneThread1.start();
        DroneThread2.start();
        DroneThread3.start();

    }
}
