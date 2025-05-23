package sysc3303.a1.group3;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import sysc3303.a1.group3.drone.Drone;
import sysc3303.a1.group3.physics.Vector2d;

public class WholeSystemTest {

    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void beforeEach() throws IOException {
        UI.setIsUIDisabled(true); // Disable UI for testing
    }

    @AfterEach
    public void afterEach() {
        System.setOut(originalOut);
    }

    // Test the whole system, similar to main
    @Test
    @Timeout(300)
    public void wholeSystemTest() {
        // Load incident and zone files from resources.
        InputStream incidentFile = Main.class.getResourceAsStream("/wholeSystemTestIncidentFile.csv");
        InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");
        String schedulerAddress = "localhost"; // Scheduler's IP
        int schedulerPort = 6014; // Scheduler's port

        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(incidentFile);
            parser.parseZoneFile(zoneFile);
        } catch (IOException e) {
            System.err.println("Failed to parse incident_file.csv or zone_location.csv, aborting.");
            e.printStackTrace();
            return;
        }

        Scheduler scheduler;
        try {
            scheduler = new Scheduler(parser.getZones(), schedulerPort);
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
        Drone drone1 = new Drone("drone1", schedulerAddress, schedulerPort, parser.getZoneMap());
        Drone drone2 = new Drone("drone2", schedulerAddress, schedulerPort, parser.getZoneMap());
        Drone drone3 = new Drone("drone3", schedulerAddress, schedulerPort, parser.getZoneMap());

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
        } catch (InterruptedException e) {}

        DroneThread2.start();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {}

        DroneThread1.start();

        // Wait for the system to complete
        try {
            FIsubsystemThread.join();
            DroneThread1.join();
            DroneThread2.join();
            DroneThread3.join();
        } catch (InterruptedException e) {
            System.err.println("Thread interrupted while waiting for completion. Exception: " + e);
        }

        try {
            scheduler.shutOff();
            Thread.sleep(5000);
            scheduler.closeSockets();
        } catch (InterruptedException e) {
            System.err.println("Failed to close sockets. Exception: " + e);
        }
        
    }
}
