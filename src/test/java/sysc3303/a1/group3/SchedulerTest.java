package sysc3303.a1.group3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import sysc3303.a1.group3.drone.Drone;
import sysc3303.a1.group3.drone.DroneRecord;
import sysc3303.a1.group3.physics.Vector2d;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    private Scheduler scheduler;
    private FireIncidentSubsystem fiSubsystem;

    private static final String schedulerAddress = "localhost"; // Scheduler's IP
    private static final int schedulerPort = 6013; // Scheduler's main port

    private DatagramSocket testSocket;

    final InputStream incidentFile = Main.class.getResourceAsStream("/incidentFile.csv");
    final InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");

    private List<Zone> zones;
    private Map<Integer, Zone> zoneMap;

    @BeforeEach
    void beforeEach() throws IOException {
        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(incidentFile);
            parser.parseZoneFile(zoneFile);
        } catch (IOException e) {
            System.err.println("Failed to parse incidentFile.csv or zone_location.csv, aborting.");
            e.printStackTrace();
            return;
        }

        scheduler = new Scheduler(parser.getZones(), schedulerPort);
        fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);

        // Create a UDP socket for testing
        testSocket = new DatagramSocket();
    }
    @AfterEach
    void closeAll() {
        if (testSocket != null && !testSocket.isClosed()) {
            testSocket.close();
        }
        if (scheduler != null) {
            scheduler.closeSockets();  // Ensure this method exists in Scheduler to close both sockets.
        }
    }


    @Test
    @Timeout(5)
    void testReceiveSubsystemEvent() throws Exception {
        // Simulate sending a SUBSYSTEM_EVENT to the scheduler
        String eventJson = "{\"zoneId\":1,\"eventType\":\"FIRE_DETECTED\",\"severity\":\"HIGH\"}";
        sendUdpMessage("SUBSYSTEM_EVENT:" + eventJson);

        // Wait briefly to allow processing
        Thread.sleep(500);

        // Verify that the event was added to scheduler
        assertFalse(scheduler.getDroneMessages().isEmpty(), "Event queue should not be empty.");
    }

    @Test
    @Timeout(5)
    void testRegisterNewDroneListener() throws Exception {
        // Simulate a new drone registering its listener info
        try {
            String message = "NEW_DRONE_LISTENER,drone1,DroneIdle,10.5,20.5";
            sendUdpMessage(message);

            // Wait briefly
            Thread.sleep(500);

            // Verify that the drone was added to the scheduler
            DroneRecord drone = scheduler.getDroneByName("drone1");
            assertNotNull(drone, "Drone should be registered.");
            assertEquals(10.5, drone.getPosition().getX(), 0.1, "Drone X position should match.");
            assertEquals(20.5, drone.getPosition().getY(), 0.1, "Drone Y position should match.");
        } catch (IllegalArgumentException e){}
    }

    @Test
    @Timeout(5)
    void testRegisterNewDronePort() throws Exception {
        // Simulate a new drone registering its main socket info
        try {
            String message = "NEW_DRONE_PORT,drone1,DroneIdle,10.5,20.5";
            sendUdpMessage(message);

            // Wait briefly
            Thread.sleep(500);

            // Verify that the drone was updated in the scheduler
            DroneRecord drone = scheduler.getDroneByName("drone1");
            assertNotNull(drone, "Drone should exist.");
            assertEquals(10.5, drone.getPosition().getX(), 0.1, "Drone X position should match.");
            assertEquals(20.5, drone.getPosition().getY(), 0.1, "Drone Y position should match.");
        } catch (IllegalArgumentException e){}
    }

    @Test
    @Timeout(5)
    void testShutdownMessage() throws Exception {
        // Simulate a shutdown request
        try {
            sendUdpMessage("SHUTDOWN");
        } catch (IllegalArgumentException e) {}
        // Wait briefly
        Thread.sleep(500);

        // Verify that the system shut down
        // Error or busy wait will happen if so.
    }

    @Test
    @Timeout(10)
    void testDronesReceiveShutoff() throws Exception {
        // Simulate adding a drone first
        sendUdpMessage("NEW_DRONE_LISTENER,drone1,DroneIdle,10.0,10.0");

        // Wait for processing
        Thread.sleep(500);

        // Simulate sending shutdown
        sendUdpMessage("SHUTDOWN");

        // Wait for processing
        Thread.sleep(1000);

        // Verify if the shutdown message was processed
        // Will timeout if not proper shutdown
    }

    @Test
    @Timeout(5)
    void testCorruptedMessageResponse() throws Exception {
        //Example of a corrupted message
        String corruptedMessage = "DOE_RQENT";

        byte[] data = corruptedMessage.getBytes();
        InetAddress schedulerAddress = InetAddress.getByName(SchedulerTest.schedulerAddress);
        DatagramPacket packet = new DatagramPacket(data, data.length, schedulerAddress, schedulerPort);
        testSocket.send(packet);

        data = new byte[1024];
        packet = new DatagramPacket(data, data.length);
        testSocket.receive(packet);

        String message = new String(packet.getData(), 0, packet.getLength());

        assertEquals( "NOT_RECEIVED", message);
    }

    // Helper method to send UDP messages to the Scheduler
    private void sendUdpMessage(String message) throws Exception {
        byte[] data = message.getBytes();
        InetAddress schedulerAddress = InetAddress.getByName(SchedulerTest.schedulerAddress);
        DatagramPacket packet = new DatagramPacket(data, data.length, schedulerAddress, schedulerPort);
        testSocket.send(packet);
    }
}
