package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.Drone;
import sysc3303.a1.group3.drone.DroneSpecifications;

import java.io.IOException;
import java.io.InputStream;

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

        Scheduler scheduler;
        try {
            scheduler = new Scheduler(parser.getZones());
        } catch (IOException e) {
            System.err.println("Failed to create scheduler, aborting.");
            e.printStackTrace();
            return;
        }

        // Create Fire Incident Subsystem
        FireIncidentSubsystem fiSubsystem;
        fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);

        // Create three drone instances.
        // No need to add them to scheduler anymore since they send sockets automatically
        Drone drone1 = new Drone("drone1", scheduler, schedulerAddress, schedulerPort, parser.getZones());
        Drone drone2 = new Drone("drone2", scheduler, schedulerAddress, schedulerPort, parser.getZones());
        Drone drone3 = new Drone("drone3", scheduler, schedulerAddress, schedulerPort, parser.getZones());

        // Create threads for the subsystem and each drone.
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread1 = new Thread(drone1, "Drone1");
        Thread DroneThread2 = new Thread(drone2, "Drone2");
        Thread DroneThread3 = new Thread(drone3, "Drone3");

        // Start all threads.
        FIsubsystemThread.start();
        DroneThread1.start();
        DroneThread2.start();
        DroneThread3.start();
    }
}
