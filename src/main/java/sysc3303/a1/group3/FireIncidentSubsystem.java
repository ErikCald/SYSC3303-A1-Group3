package sysc3303.a1.group3;

package sysc3303.a1.group3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents the Fire Incident Subsystem, which simulates fire incidents and communicates with the Scheduler.
 * This subsystem reads fire incident events from an input file, sends them to the Scheduler, and waits for
 * acknowledgments.
 */
public class FireIncidentSubsystem {
    private static final int SCHEDULER_PORT = 5000; // Port number for the Scheduler
    private static final String SCHEDULER_ADDRESS = "localhost"; // Address of the Scheduler
    private static final int BUFFER_SIZE = 1024; // Buffer size for receiving data

    private DatagramSocket socket; // Socket for UDP communication
    private InetAddress schedulerAddress; // Address of the Scheduler

    /**
     * Constructs a FireIncidentSubsystem instance and initializes the socket.
     *
     * @throws SocketException if there is an error creating the socket
     * @throws UnknownHostException if the Scheduler address cannot be resolved
     */
    public FireIncidentSubsystem() throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        schedulerAddress = InetAddress.getByName(SCHEDULER_ADDRESS);
    }

    /**
     * Starts the Fire Incident Subsystem by reading events from the specified input file.
     *
     * @param inputFile The path to the input file containing fire incident events
     */
    public void start(String inputFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Event event = parseEvent(line); // Parse each line into an Event object
                sendEventToScheduler(event); // Send the event to the Scheduler
                receiveAcknowledgment(); // Wait for acknowledgment from the Scheduler
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a line from the input file into an Event object.
     *
     * @param line The line to parse
     * @return An Event object representing the parsed data
     */
    private Event parseEvent(String line) {
        String[] parts = line.split("\\s+"); // Split the line by whitespace
        LocalTime time = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm:ss")); // Parse time
        int zoneId = Integer.parseInt(parts[1]); // Parse zone ID
        EventType eventType = EventType.valueOf(parts[2]); // Parse event type
        Severity severity = Severity.valueOf(parts[3]); // Parse severity level
        return new Event(time.toString(), zoneId, eventType, severity); // Create and return Event object
    }

    /**
     * Sends an Event object to the Scheduler using UDP.
     *
     * @param event The Event object to send
     * @throws IOException if there is an error sending the packet
     */
    private void sendEventToScheduler(Event event) throws IOException {
        byte[] sendData = event.toString().getBytes(); // Convert event to bytes
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, schedulerAddress, SCHEDULER_PORT);
        socket.send(sendPacket); // Send packet to Scheduler
        System.out.println("Sent event to Scheduler: " + event); // Log sent event
    }

    /**
     * Receives acknowledgment from the Scheduler after sending an event.
     *
     * @throws IOException if there is an error receiving the packet
     */
    private void receiveAcknowledgment() throws IOException {
        byte[] receiveData = new byte[BUFFER_SIZE]; // Buffer for receiving data
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket); // Wait for acknowledgment packet
        String acknowledgment = new String(receivePacket.getData(), 0, receivePacket.getLength()); // Convert bytes to string
        System.out.println("Received acknowledgment: " + acknowledgment); // Log acknowledgment received
    }

}

