package sysc3303.a1.group3;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.List;
import java.time.Duration;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Represents the Fire Incident Subsystem, which reads fire incident events from an input file and sends them
 * to the Scheduler via UDP. It sends each event prefixed with "SUBSYSTEM_EVENT:" and, when done, sends a "SHUTDOWN" signal.
 */
public class FireIncidentSubsystem implements Runnable {
    volatile int elapsedSeconds;
    private final Scheduler scheduler;
    private List<Event> events;
    private List<Zone> zones;
    private int eventCount;
    Instant startTime;

    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private int schedulerPort;

    public FireIncidentSubsystem(Scheduler s, InputStream fileStream, String schedulerAddress, int schedulerPort) throws IOException {
        this.scheduler = s;
        eventCount = 0;
        events = new ArrayList<>();
        startTime = Instant.now();
        Parser parser = new Parser();
        if (fileStream == null) {
            System.out.println("Incident file doesn't exist");
            return;
        }
        events = parser.parseIncidentFile(fileStream);

        try {
            this.socket = new DatagramSocket();
            this.schedulerAddress = InetAddress.getByName(schedulerAddress);
            this.schedulerPort = schedulerPort;
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        for (Event event: events) {
            // Busy-wait until the event's scheduled time (in seconds) is reached.
            do {
                Instant currentTime = Instant.now();
                elapsedSeconds = (int) Duration.between(startTime, currentTime).getSeconds();
            } while (elapsedSeconds <= event.getTime());

            ++eventCount;
            System.out.println("Sending Event " + eventCount + " to Scheduler: \n" + event + "\n");
            // Send event via UDP with a prefix.
            String eventData = "SUBSYSTEM_EVENT:" + convertEventToJson(event);
            byte[] sendData = eventData.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Error sending event from subsystem: " + e.getMessage());
            }
        }
        System.out.println("All events have been exhausted, " + Thread.currentThread().getName() + ", is closing.");
        sendShutOffSignal();
    }

    // Sends a shutdown signal via UDP.
    private void sendShutOffSignal() {
        String shutdownMessage = "SHUTDOWN";
        byte[] sendData = shutdownMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending shutdown signal: " + e.getMessage());
        }
    }

    // Converts an Event object to a JSON string.
    private String convertEventToJson(Event event) {
        return String.format("{\"time\":%d, \"zoneId\":%d, \"eventType\":\"%s\", \"severity\":\"%s\"}",
            event.getTime(), event.getZoneId(), event.getEventType(), event.getSeverity());
    }

    public void manageResponse(Event event) {
        System.out.println("The drone confirmed it has received the event: " + event + "\n");
    }

    public List<Event> getEvents() { return events; }
}
