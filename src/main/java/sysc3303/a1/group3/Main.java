package sysc3303.a1.group3;

import java.io.InputStream;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // fileStream for scheduler, parses events in constructor
        InputStream fileStream = Main.class.getResourceAsStream("/incidentFile.csv");

        //Create required objects for simulation
        Scheduler scheduler = new Scheduler();
        FireIncidentSubsystem fiSubsystem = new FireIncidentSubsystem(scheduler, fileStream);
        Drone drone1 = new Drone(scheduler);

        //Ensure scheduler aggregation is complete
        scheduler.addDrone(drone1);
        scheduler.setSubsystem(fiSubsystem);

        //Make threads from the aforementioned objects
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread = new Thread((drone1), "Drone1");

        // Wait for user to press Enter before starting
        System.out.println("\nPress Enter to start the simulation:");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        scanner.close();

        // Start the simulation
        FIsubsystemThread.start();
        DroneThread.start();

    }
}
