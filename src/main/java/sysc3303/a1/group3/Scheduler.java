package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Scheduler {

    private final int MAX_SIZE = 10;
    // Queue for events coming from the subsystem.
    private final Queue<Event> droneMessages;
    private boolean droneMessagesWritable;
    private boolean droneMessagesReadable;

    // (For potential confirmation messages from the subsystem.)
    private final Queue<Event> incidentSubsystemQueue;
    private boolean incidentSubsystemWritable;
    private boolean incidentSubsystemReadable;

    private final List<DroneRecord> drones;
    private final List<Zone> zones;

    private volatile boolean shutoff;

    private DatagramSocket socket;
    int schedulerPort = 5000;

    public Scheduler(InputStream zoneFile) throws IOException {
        this.droneMessages = new ArrayDeque<>();
        this.incidentSubsystemQueue = new ArrayDeque<>();
        this.droneMessagesWritable = true;
        this.droneMessagesReadable = false;
        this.incidentSubsystemWritable = true;
        this.incidentSubsystemReadable = false;
        this.drones = new ArrayList<>();

        Parser parser = new Parser();
        if (zoneFile == null) {
            zones = new ArrayList<>();
            System.out.println("Zone file doesn't exist");
        } else {
            zones = parser.parseZoneFile(zoneFile);
        }
        shutoff = false;

        try {
            this.socket = new DatagramSocket(schedulerPort);
        } catch (SocketException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            throw new RuntimeException(e);
        }

        startUDPListener();
    }

    // Starts one UDP listener thread that continuously receives packets.
    private void startUDPListener() {
        new Thread(() -> {
            byte[] receiveData = new byte[1024];
            while (!shutoff) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                try {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.startsWith("SUBSYSTEM_EVENT:")) {
                        // Received an event from the Fire Incident Subsystem.
                        String json = message.substring("SUBSYSTEM_EVENT:".length());
                        Event event = convertJsonToEvent(json);
                        addEvent(event);
                    } else if (message.equals("DRONE_REQ_EVENT")) {
                        // Received a request from a drone.
                        InetAddress droneAddress = packet.getAddress();
                        int dronePort = packet.getPort();
                        new Thread(() -> {
                            Event event = removeEvent();
                            String eventData;
                            if (event != null) {
                                eventData = convertEventToJson(event);
                            } else {
                                eventData = "NO_EVENT";
                            }
                            DatagramPacket response = new DatagramPacket(eventData.getBytes(), eventData.getBytes().length, droneAddress, dronePort);
                            try {
                                socket.send(response);
                            } catch (IOException e) {
                                System.err.println("Error sending event to drone: " + e.getMessage());
                            }
                        }).start();
                    } else if (message.equals("SHUTDOWN")) {
                        shutOff();
                    } else if (message.startsWith("NEW_DRONE")) {
                        String[] parts = message.split(",");
                        String name = parts[1];
                        String state = parts[2];
                        try {
                            DroneRecord newDrone = new DroneRecord(name, state, packet.getPort(), packet.getAddress());
                            System.out.println("New Drone Registered: " + newDrone.getDroneName() + " at address " + newDrone.getAddress() + " and port:" + newDrone.getPort());
                            drones.add(newDrone);
                        } catch (Exception e) {
                            System.err.println("Failed to parse NEW_DRONE message: " + e.getMessage());
                        }

                    } if (message.startsWith("STATE_CHANGE")){
                        String[] parts = message.split(",");
                        String name = parts[1];
                        String state = parts[2];

                        for (DroneRecord record : drones) {
                            if (record.getDroneName().equals(name)) {
                                record.setState(state);
                                break;
                            }
                        }

                    }
                    // Additional message types (e.g., drone state updates) can be handled here.
                } catch (IOException e) {
                    System.err.println("Error in UDP listener: " + e.getMessage());
                }
            }
        }).start();
    }

    // Synchronized method to add an event to the queue.
    public synchronized void addEvent(Event event) {
        if (shutoff) {
            return;
        }
        while (!droneMessagesWritable) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        if (shutoff) {
            return;
        }

        // Add 2 copies of the event if it requires 2 drones.
        if (event.getSeverity() == Severity.High || event.getSeverity() == Severity.Moderate){
            droneMessages.add(event);
        }

        droneMessages.add(event);
        droneMessagesReadable = true;
        if (droneMessages.size() >= MAX_SIZE) {
            droneMessagesWritable = false;
        }
        notifyAll();
    }

    // Synchronized method to remove (i.e. get) the next available event.
    public synchronized Event removeEvent() {
        while (droneMessages.isEmpty() && !shutoff) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        if (shutoff) {
            return null;
        }
        if (droneMessages.isEmpty()){
            return null;
        }
        Event event = droneMessages.remove();
        droneMessagesWritable = true;
        if (droneMessages.isEmpty()) {
            droneMessagesReadable = false;
        }
        notifyAll();
        return event;
    }

    public boolean confirmDroneInZone(Drone drone) {
        // For iteration 1, always return true.
        return true;
    }


    // Synchronized shutdown method using wait/notifyAll.
    public synchronized void shutOff() {
        while (!droneMessages.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        this.shutoff = true;

        sendShutoffToDrones();
//        socket.close();

        notifyAll();
    }

    // Sends "SHUTOFF" to all drones
    private void sendShutoffToDrones() {
        String message = "SHUTOFF";
        byte[] sendData = message.getBytes();

        for (DroneRecord drone : drones) {
            try {
                InetAddress droneAddress = drone.getAddress();
                int dronePort = drone.getPort();
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, droneAddress, dronePort);
                socket.send(packet);
                System.out.println("Sent SHUTOFF to drone: " + drone.getDroneName() + " at " + droneAddress + ":" + dronePort);
            } catch (IOException e) {
                System.err.println("Failed to send SHUTOFF to drone: " + drone.getDroneName());
            }
        }
    }



    // HELPERS, GETTERS, SETTERS:

    // Converts an Event object to a JSON string.
    private String convertEventToJson(Event event) {
        return String.format("{\"time\":%d, \"zoneId\":%d, \"eventType\":\"%s\", \"severity\":\"%s\"}",
            event.getTime(), event.getZoneId(), event.getEventType(), event.getSeverity());
    }

    // Very simple JSON parser assuming a fixed format.
    private Event convertJsonToEvent(String json) {
        json = json.trim();
        json = json.substring(1, json.length() - 1); // remove { and }
        String[] tokens = json.split(",");
        int time = 0;
        int zoneId = 0;
        String eventType = "";
        String severity = "";
        for (String token : tokens) {
            String[] pair = token.split(":");
            if (pair.length < 2) continue;
            String key = pair[0].trim().replaceAll("\"", "");
            String value = pair[1].trim().replaceAll("\"", "");
            switch (key) {
                case "time":
                    time = Integer.parseInt(value);
                    break;
                case "zoneId":
                    zoneId = Integer.parseInt(value);
                    break;
                case "eventType":
                    eventType = value;
                    break;
                case "severity":
                    severity = value;
                    break;
            }
        }
        String timeStr = convertSecondsToTimeString(time);
        return new Event(java.sql.Time.valueOf(timeStr), zoneId, Event.EventType.fromString(eventType), Severity.fromString(severity));
    }

    // Helper to convert seconds to a "hh:mm:ss" string.
    private String convertSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
