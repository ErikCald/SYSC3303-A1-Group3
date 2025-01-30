package sysc3303.a1.group3;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        //Create required objects for simulation
        Scheduler scheduler = new Scheduler();
        FireIncidentSubsystem fiSubsystem = new FireIncidentSubsystem(scheduler);
        Drone drone1 = new Drone(scheduler);

        //Ensure scheduler aggregation is complete
        scheduler.addDrone(drone1);
        scheduler.setSubsystem(fiSubsystem);

        //Make threads from the aforementioned objects
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread = new Thread((drone1), "Drone1");

        // Call parseEvents() to parse csv into Event Objects
        fiSubsystem.parseEvents();

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
