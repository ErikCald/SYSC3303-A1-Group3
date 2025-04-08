package sysc3303.a1.group3.drone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.Zone;
import sysc3303.a1.group3.physics.Vector2d;
import sysc3303.a1.group3.UI;

class DroneTest {
    private Scheduler scheduler;
    private Drone drone;
    private List<Zone> zones;
    private Map<Integer, Zone> zoneMap;
    private DatagramSocket schedulerSocket;
    private DatagramSocket droneSocket;
    private InetAddress schedulerAddress;
    private int schedulerPort = 6002;

    @BeforeEach
    void setUp() throws IOException {
        UI.setIsUIDisabled(true); // Disable UI for testing
        
        // Set up the Scheduler that listens for requests
        schedulerSocket = new DatagramSocket(schedulerPort);
        schedulerAddress = InetAddress.getByName("localhost");

        // Mock zones (simple zones for testing)
        zones = new ArrayList<>();
        zones.add(new Zone(1, 0, 0, 10, 10, Vector2d.of(5, 5)));
        zoneMap = zones.stream().collect(Collectors.toMap(Zone::zoneID, z -> z));

        // Initialize the Drone with real sockets
        droneSocket = new DatagramSocket();
        drone = new Drone("drone1", "localhost", schedulerPort, zoneMap);

        // Start the scheduler
        new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (true) {
                    DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                    schedulerSocket.receive(packet);

                    // Simulate the Scheduler response based on the message
                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.equals("DRONE_REQ_EVENT")) {
                        String eventJson = "{\"time\":5, \"zoneId\":1, \"eventType\":\"FIRE_DETECTED\", \"severity\":\"HIGH\"}";
                        String response = eventJson;
                        byte[] sendData = response.getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(sendData, sendData.length, packet.getAddress(), packet.getPort());
                        schedulerSocket.send(responsePacket);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @AfterEach
    void closeAll() {
        if (droneSocket != null && !droneSocket.isClosed()) {
            drone.closeSockets();
        }
        if (schedulerSocket != null && !schedulerSocket.isClosed()) {
            schedulerSocket.close();
        }
    }


    @Test
    @Timeout(5)
    void testRequestNewEvent_successfulEvent() throws Exception {
        // Start the Drone in its own thread (simulating real-time behavior)
        Thread droneThread = new Thread(drone);
        droneThread.start();

        // Simulate the Drone requesting a new event
        Optional<Event> event = drone.requestNewEvent();

        // Wait briefly for the event to be received
        Thread.sleep(500);

        // If scheduler does not send an exception, then it passed
    }

    @Test
    @Timeout(5)
    void testFillWaterTank() throws InterruptedException {
        // Create WaterTank and reduce its level to simulate usage
        WaterTank waterTank = new WaterTank();

        // Reduce water level
        waterTank.reduceWaterLevel("drone1");
        waterTank.reduceWaterLevel("drone1");

        // Check if the water tank is not full initially
        assertEquals(13, waterTank.getWaterLevel(), "Initial water level should be 13");

        // Now call fillWaterTank to fill the water tank
        waterTank.fillWaterLevel();

        // Check if the water tank is full now
        assertEquals(15, waterTank.getWaterLevel(), "Water level should be 15 after filling");
    }

    @Test
    void testCorruptedMessage(){
        String request = "DRONE_REQ_EVENT";
        request = drone.corruptMessage(request);
        assertNotEquals(request, "DRONE_REQ_EVENT");
    }
}
