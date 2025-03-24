package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.Drone;
import sysc3303.a1.group3.physics.Vector2d;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<Integer, Zone> zoneMap = zones.stream()
            .collect(Collectors.toMap(Zone::zoneID, z -> z));


        Scheduler scheduler;
        try {
            scheduler = new Scheduler(zones, schedulerPort);
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
        Drone drone1 = new Drone("drone1", schedulerAddress, schedulerPort, zoneMap);
        Drone drone2 = new Drone("drone2", schedulerAddress, schedulerPort, zoneMap);
        Drone drone3 = new Drone("drone3", schedulerAddress, schedulerPort, zoneMap);

        drone3.setPosition(new Vector2d(3, 3));
        drone2.setPosition(new Vector2d(2, 2));
        drone1.setPosition(new Vector2d(1, 1));


        // Create threads for the subsystem and each drone.
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread1 = new Thread(drone1, "Drone1");
        Thread DroneThread2 = new Thread(drone2, "Drone2");
        Thread DroneThread3 = new Thread(drone3, "Drone3");

        // Start all threads.
        FIsubsystemThread.start();
        DroneThread3.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        DroneThread2.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        DroneThread1.start();
    }
}
