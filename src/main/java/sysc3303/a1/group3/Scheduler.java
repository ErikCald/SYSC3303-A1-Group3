package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.*;
import sysc3303.a1.group3.physics.Vector2d;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Vector;

public class Scheduler {

    private final int MAX_SIZE = 10;

    // Queue for events coming from the subsystem.
    private final Queue<Event> droneMessages = new ArrayDeque<>();
    private boolean droneMessagesWritable = true;
    private boolean droneMessagesReadable = false;

    // Drone List is now a List of DroneRecords to hold info about drones the scheulder has contact to
    private final List<DroneRecord> drones = Collections.synchronizedList(new ArrayList<>());

    private final List<Zone> zones;

    // Boolean for if the system should shut down
    private volatile boolean shutoff = false;

    // Boolean for if all drones have no events, only to be set after shutdown is true.
    // Ensures that the scheduler doesn't shut down while the drone still needs to send packets to it.
    private volatile boolean allDronesShutoff = false;

    // general purpose socket
    private DatagramSocket socket;

    // socket to exclusive send packets when the main socket is busy listening
    private DatagramSocket sendSocket;

    /**
     * Creates a new Scheduler
     *
     * @param zones the zoneFile containing the fire zones
     * @throws SocketException if any of the required sockets could not be created
     */
    public Scheduler(List<Zone> zones, int schedulerPort) throws SocketException {
        Objects.requireNonNull(zones, "ListOfZones");
        this.zones = zones;

        this.socket = new DatagramSocket(schedulerPort);
        this.sendSocket = new DatagramSocket();

        // start the main listener/handler
        startUDPListener();

    }

    // Starts one UDP listener thread that continuously receives packets.
    // Based on the packet info, respond and react accordingly
    private void startUDPListener() {
        new Thread(() -> {
            byte[] receiveData = new byte[1024];
            while (!shutoff || !allDronesShutoff) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                try {
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.startsWith("SUBSYSTEM_EVENT:")) {
                        // Received an event from the Fire Incident Subsystem.
                        String json = message.substring("SUBSYSTEM_EVENT:".length());
                        Event event = convertJsonToEvent(json);
                        addEvent(event);
                    } else if (message.startsWith("DRONE_REQ_EVENT")) {
                        // Received a request from a drone.
                        new Thread(() -> {
                            try {
                                removeEvent(packet.getAddress(), packet.getPort());
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }, "Scheduler-RemoveEvent").start();
                    } else if (message.equals("SHUTDOWN")) {
                        // Received a shutdown request from FiSubsystem.
                        new Thread(() -> {
                            try {
                                shutOff();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    } else if (message.startsWith("NEW_DRONE_LISTENER")) {
                        // New Drone registering, the one has information about its listener socket
                        String[] parts = message.split(",");
                        String name = parts[1];
                        String state = parts[2];
                        double x = Double.parseDouble(parts[3]);
                        double y = Double.parseDouble(parts[4]);
                        if (getDroneByName(name) == null){
                            // If record doesn't exist, add it
                            DroneRecord newDrone = new DroneRecord(name, state, x, y);
                            newDrone.setListenerAddress(packet.getAddress());
                            newDrone.setListenerPort(packet.getPort());
                            drones.add(newDrone);
                        } else {
                            // If record exists, then update it
                            getDroneByName(name).setListenerAddress(packet.getAddress());
                            getDroneByName(name).setListenerPort(packet.getPort());
                        }

                    } else if (message.startsWith("NEW_DRONE_PORT")){
                        // New Drone registering, the one has information about its main drone socket
                        String[] parts = message.split(",");
                        String name = parts[1];
                        String state = parts[2];
                        double x = Double.parseDouble(parts[3]);
                        double y = Double.parseDouble(parts[4]);
                        if (getDroneByName(name) == null){
                            // If record doesn't exist, add it
                            DroneRecord newDrone = new DroneRecord(name, state, x, y);
                            newDrone.setDroneAddress(packet.getAddress());
                            newDrone.setDronePort(packet.getPort());
                            drones.add(newDrone);
                        } else {
                            // If record exists, then update it
                            getDroneByName(name).setDroneAddress(packet.getAddress());
                            getDroneByName(name).setDronePort(packet.getPort());
                        }
                    } if (message.startsWith("STATE_CHANGE")) {
                        // Drone wants to update scheduler about a state change
                        synchronized (drones){
                            String[] parts = message.split(",");
                            String name = parts[1];
                            String state = parts[2];

                            // Update the drone records accordingly
                            setDroneStateByName(name, state);
                            if (Objects.equals(state, "DroneIdle")){
                                getDroneByName(name).setEvent(null);
                                if (areAllDroneEventsNull() && shutoff){
                                    allDronesShutoff = true;
                                }
                            }

                            // Confirm with the drone that it updated the info and the drone can continue to run.
                            String confirm = "STATE_CHANGE_OK";
                            byte[] confirmData = confirm.getBytes();
                            DatagramPacket confirmChangePacket = new DatagramPacket(confirmData, confirmData.length, packet.getAddress(), packet.getPort());
                            socket.send(confirmChangePacket);
                        }
                    }
                    // Additional message types (e.g., drone state updates) can be handled here.
                } catch (IOException e) {
                    break;
                }
            }
            socket.close();
            sendSocket.close();
        }, "Scheduler-UPDListener").start();
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
        // Temp commented out for debug
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
    public synchronized Event removeEvent(InetAddress originalAddress, int originalPort) throws InterruptedException {
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


        // Get the event and re-distribute events as needed (scheduling algorithm)
        Event event = droneMessages.remove();
        List<DroneRecord> availableDrones = getAvailableDrones();

        distributeEvent(event, availableDrones, originalAddress, originalPort);

        // Small delay to ensure drone states update, can be smaller, but I kept it as 1 second to be easier for bug fixing for now
        Thread.sleep(500);

        droneMessagesWritable = true;
        if (droneMessages.isEmpty()) {
            droneMessagesReadable = false;
        }
        notifyAll();
        return event;
    }

    // Give the newest event to the drone closest to the zone
    // If that drone already had an event, give that event to the second-closest drone to it
    private void distributeEvent(Event event, List<DroneRecord> availableDrones, InetAddress originalAddress, int originalPort) {
        if (availableDrones.isEmpty()) {
            System.out.println("No available drones to assign the event. Something has gone wrong!");
            return;
        }

        DroneRecord selectedDrone;
        String selectedDroneName;
        Event previousEvent = null;
        String redistributedDrone = null;
        String eventData;
        if (event != null) {
            eventData = convertEventToJson(event);
        } else {
            eventData = "NO_EVENT";
        }

        if (availableDrones.size() == 1) {
            String originalDroneName = getDroneByPort(originalPort).getDroneName();
            if (getDroneByName(originalDroneName).getEvent() != null) {
                System.err.println("ERROR: Did a drone just ask for an event, but it already have one? Or some other error?");
            } else {
                //send packet to the drone:
                DatagramPacket response = new DatagramPacket(eventData.getBytes(), eventData.getBytes().length, originalAddress, originalPort);
                try {
                    sendSocket.send(response);
                    setDroneEventByName(originalDroneName, event);
                    System.out.println(originalDroneName + " is scheduled with event, " + event + "\n");
                } catch (IOException e) {
                    System.err.println("Error sending event to drone: " + e.getMessage());
                }
            }
        } else {
            // If multiple drones are available, find the one closest to the zone
            selectedDroneName = findClosestDrone(availableDrones, event).getDroneName();
            previousEvent = getDroneEventByName(selectedDroneName);

            if (selectedDroneName != null && ifNewEventFurther(event, previousEvent)) {
                // if the drone already had an event, redistribute

                // First, send the original drone the redistributed event
                previousEvent = getDroneEventByName(selectedDroneName);
                String secondEventData = convertEventToJson(previousEvent);
                DatagramPacket response = new DatagramPacket(secondEventData.getBytes(), secondEventData.getBytes().length, originalAddress, originalPort);

                try {
                    String originalDroneName = getDroneByPort(originalPort).getDroneName();
                    sendSocket.send(response);
                    setDroneEventByName(originalDroneName, previousEvent);
                    System.out.println(originalDroneName + " is re-scheduled with older event, " + previousEvent);
                } catch (IOException e) {
                    System.err.println("Error sending event to drone: " + e.getMessage());
                }

                // Next, send the newest event to the closer drone:
                response = new DatagramPacket(eventData.getBytes(), eventData.getBytes().length, getListenerAddressByName(selectedDroneName), getListenerPortByName(selectedDroneName));
                try {
                    sendSocket.send(response);
                    setDroneEventByName(selectedDroneName, event);
                    System.out.println(selectedDroneName + " is scheduled with newer event, " + event + "\n");
                } catch (IOException e) {
                    System.err.println("Error sending event to drone: " + e.getMessage());
                }
                return;
            }
            //send packet to the drone:
            String originalDroneName = getDroneByPort(originalPort).getDroneName();
            DatagramPacket response = new DatagramPacket(eventData.getBytes(), eventData.getBytes().length, originalAddress, originalPort);
            try {
                sendSocket.send(response);
                setDroneEventByName(originalDroneName, event);
                System.out.println(originalDroneName + " is scheduled with event, " + event + "\n");
            } catch (IOException e) {
                System.err.println("Error sending event to drone: " + e.getMessage());
            }
        }
    }

    // get all drones that are idle or EnRoute
    private List<DroneRecord> getAvailableDrones() {
        List<DroneRecord> availableDrones = new ArrayList<>();

        // Iterate over the drones list and add those in 'DroneIdle' or 'DroneEnRoute' state
        synchronized (drones) {
            for (DroneRecord drone : drones) {
                String state = drone.getState();
                Event event = drone.getEvent();
                if ((state.equals("DroneIdle") || state.equals("DroneEnRoute"))) {
                    availableDrones.add(drone);
                }
            }
        }

        return availableDrones;
    }


    public boolean confirmDroneInZone(Drone drone) {
        // For current iteration when errors cannot happen yet, always return true.
        return true;
    }


    // Synchronized shutdown method using wait/notifyAll.
    public synchronized void shutOff() throws InterruptedException {
        while (!droneMessages.isEmpty() ) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        this.shutoff = true;
        notifyAll();
        sendShutoffToDrones();
    }

    // Sends "SHUTOFF" to all drones
    private void sendShutoffToDrones() throws InterruptedException {
        String message = "SHUTOFF";
        byte[] sendData = message.getBytes();

        for (DroneRecord drone : drones) {
            try {
                InetAddress droneAddress = drone.getListenerAddress();
                int dronePort = drone.getListenerPort();
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, droneAddress, dronePort);
                socket.send(packet);
                System.out.println("Sent SHUTOFF to drone: " + drone.getDroneName() + " at " + droneAddress + ":" + dronePort);

            } catch (IOException e) {
                System.err.println("Failed to send SHUTOFF to drone: " + drone.getDroneName());
            }
        }
        Thread.sleep(1000);
        for (DroneRecord drone : drones) {
            try {
                message = "NO_EVENT";
                sendData = message.getBytes();
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, drone.getDroneAddress(), drone.getDronePort());
                socket.send(packet);
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
        return new Event(Time.valueOf(timeStr), zoneId, Event.EventType.fromString(eventType), Severity.fromString(severity));
    }

    // Helper to convert seconds to a "hh:mm:ss" string.
    private String convertSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void setDroneStateByName(String droneName, String newState) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                drone.setState(newState);
                return;
            }
        }
    }
    public void setDroneEventByName(String droneName, Event newEvent) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                drone.setEvent(newEvent);
                return;
            }
        }
    }
    public Event getDroneEventByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone.getEvent();
            }
        }
        return null;
    }
    public InetAddress getListenerAddressByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone.getListenerAddress();
            }
        }
        return null;  // Return null if droneName is not found
    }
    public int getListenerPortByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone.getListenerPort();
            }
        }
        return -1;  // Return -1 if droneName is not found
    }
    public DroneRecord getDroneByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone;
            }
        }
        return null;
    }
    public DroneRecord getDroneByPort(int dronePort) {
        for (DroneRecord drone : drones) {
            if (drone.getDronePort() == dronePort) {
                return drone;
            }
        }
        return null;
    }
    public boolean areAllDroneEventsNull() {
        for (DroneRecord drone : drones) {
            if (drone.getEvent() != null) {
                return false;
            }
        }
        return true; // All elements were null
    }
    public Queue<Event> getDroneMessages() {
        return droneMessages;
    }


    private DroneRecord findClosestDrone(List<DroneRecord> availableDrones, Event event) {
        if (availableDrones.isEmpty()) {
            return null; // No available drones
        }

        DroneRecord closestDrone = null;
        double minDistance = Double.MAX_VALUE;

        for (DroneRecord drone : availableDrones) {
            double distance = getDistanceFromZone(drone.getPosition(), event);

            if (distance < minDistance) {
                minDistance = distance;
                closestDrone = drone;
            }
        }

        return closestDrone;
    }

    //Should be changed when movement and location for drones is implemented.
    //Gets the distance from the zone to the event's zone
    private double getDistanceFromZone(Vector2d dronePosition, Event event) {
        Zone eventZone = zones.stream()
            .filter(z -> z.zoneID() == event.getZoneId())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Zone not found for zoneId: " + event.getZoneId()));

        return eventZone.centre().subtract(dronePosition).magnitude();
    }

    private boolean ifNewEventFurther(Event newEvent, Event olderEvent) {
        if (olderEvent == null || newEvent == null) {
            return false;
        }

        Zone newEventZone = zones.stream()
            .filter(z -> z.zoneID() == newEvent.getZoneId())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Zone not found for zoneId: " + newEvent.getZoneId()));

        Zone olderEventZone = zones.stream()
            .filter(z -> z.zoneID() == olderEvent.getZoneId())
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Zone not found for zoneId: " + olderEvent.getZoneId()));

        // Compute distances from the base (0,0)
        double newEventDistance = newEventZone.centre().magnitude();
        double olderEventDistance = olderEventZone.centre().magnitude();

        // Return true if the new event is further away than the older event
        return newEventDistance > olderEventDistance;
    }

    public void closeSockets() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
    }



}
