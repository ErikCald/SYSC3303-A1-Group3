package sysc3303.a1.group3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sysc3303.a1.group3.drone.Drone;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    // All package private
    Scheduler scheduler;
    Drone drone1;
    FireIncidentSubsystem fiSubsystem;
    Parser parser;

    String schedulerAddress = "localhost"; // Scheduler's IP
    int schedulerPort = 6012; // Scheduler's port

    InputStream fileStream;
    final InputStream incidentFile = Main.class.getResourceAsStream("/quickEvent.csv");
    final InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");

    @BeforeEach
    void beforeEach() throws IOException {
        parser = new Parser();

        // Ensure files are loaded correctly
        if (incidentFile == null || zoneFile == null) {
            throw new IOException("Resource files not found.");
        }

        // Parse the files and initialize objects
        parser.parseIncidentFile(incidentFile);
        parser.parseZoneFile(zoneFile);

        scheduler = new Scheduler(parser.getZones(), schedulerPort);
        drone1 = new Drone("drone1", schedulerAddress, schedulerPort, parser.getZones());
        fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);
    }

    @AfterEach
    void closeAll() {
        if (scheduler != null) {
            scheduler.closeSockets();  // Ensure this method exists in Scheduler to close both sockets.
        }
    }

    @Test
    void testParseEvents() {
        assertFalse(fiSubsystem.getEvents().isEmpty(), "The event list should not be empty after parsing.");
    }

    @Test
    @Timeout(5)
    void connectToScheduler() throws InterruptedException {
        // Start FireIncidentSubsystem in a separate thread
        Thread fiSubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        fiSubsystemThread.start();

        // Wait briefly to allow the FireIncidentSubsystem to start sending events
        Thread.sleep(3000);

        // Verify that the scheduler's message queue is not empty (i.e., events were sent)
        assertFalse(scheduler.getDroneMessages().isEmpty(), "Expected droneMessages to not be empty after subsystem sends events.");

        // Optionally, verify that the shutdown message is sent after all events are processed.
        // This could be done by verifying any logs, status, or behavior triggered by the "SHUTDOWN" signal.
    }
}
