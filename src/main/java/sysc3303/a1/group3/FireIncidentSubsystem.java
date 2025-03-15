package sysc3303.a1.group3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Represents the Fire Incident Subsystem, which reads fire incident events from an input file and sends them
 * to the Scheduler via UDP. It sends each event prefixed with "SUBSYSTEM_EVENT:" and, when done, sends a "SHUTDOWN" signal.
 */
public class FireIncidentSubsystem implements Runnable {
    volatile int elapsedSeconds;
    private List<Event> events;
    private int eventCount;
    Instant startTime;

    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private int schedulerPort;

    public FireIncidentSubsystem(List<Event> events, String schedulerAddress, int schedulerPort) {
        eventCount = 0;
        startTime = Instant.now();
        this.events = events;

        try {
            this.socket = new DatagramSocket();
            this.schedulerAddress = InetAddress.getByName(schedulerAddress);
            this.schedulerPort = schedulerPort;
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // Now sends events with sockets
    @Override
    public void run() {
        for (Event event: events) {
            // Busy-wait until the event's scheduled time (in seconds) is reached.
            do {
                Instant currentTime = Instant.now();
                elapsedSeconds = (int) Duration.between(startTime, currentTime).getSeconds();
            } while (elapsedSeconds <= event.getTime());

            ++eventCount;
            System.out.println("\nSending Event " + eventCount + " to Scheduler: \n" + event + "\n");
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
        socket.close();
    }

    // Converts an Event object to a JSON string.
    private String convertEventToJson(Event event) {
        return String.format("{\"time\":%d, \"zoneId\":%d, \"eventType\":\"%s\", \"severity\":\"%s\"}",
            event.getTime(), event.getZoneId(), event.getEventType(), event.getSeverity());
    }

    public List<Event> getEvents() { return events; }
}
