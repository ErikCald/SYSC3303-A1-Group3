package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.*;
import sysc3303.a1.group3.physics.Vector2d;

import java.io.IOException;
import java.io.InputStream;
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

public class Scheduler {

    private final int MAX_SIZE = 10;
    // Queue for events coming from the subsystem.
    private final Queue<Event> droneMessages = new ArrayDeque<>();
    private boolean droneMessagesWritable = true;
    private boolean droneMessagesReadable = false;

    // (For potential confirmation messages from the subsystem.)
    private final Queue<Event> incidentSubsystemQueue = new ArrayDeque<>();
    private boolean incidentSubsystemWritable = true;
    private boolean incidentSubsystemReadable = false;

    private final List<DroneRecord> drones = Collections.synchronizedList(new ArrayList<>());
    private final List<Zone> zones;

    private volatile boolean shutoff = false;
    private volatile boolean allDronesShutoff = false;

    private DatagramSocket socket;
    private DatagramSocket sendSocket;
    private final int schedulerPort = 5000;
    private final int sendSchedulerPort = 6000;

    /**
     * Creates a new Scheduler
     *
     * @param zoneFile the zoneFile containing the fire zones
     * @throws IOException if the zone file could not be parsed
     * @throws SocketException if any of the required sockets could not be created
     */
    public Scheduler(InputStream zoneFile) throws IOException {
        Objects.requireNonNull(zoneFile, "zoneFile");
        this.zones = new Parser().parseZoneFile(zoneFile);

        this.socket = new DatagramSocket(schedulerPort);
        this.sendSocket = new DatagramSocket(sendSchedulerPort);

        startUDPListener();
    }

    // Starts one UDP listener thread that continuously receives packets.
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
                                removeEvent();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                    } else if (message.equals("SHUTDOWN")) {
                        shutOff();
                    } else if (message.startsWith("NEW_DRONE_LISTENER")) {
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
                        synchronized (drones){
                            String[] parts = message.split(",");
                            String name = parts[1];
                            String state = parts[2];

                            setDroneStateByName(name, state);

                            // CONT HERE. DRONES NOT CLEARING EVENTS CORRECTLY?
                            if (Objects.equals(state, "DroneIdle")){
                                getDroneByName(name).setEvent(null);
                                System.out.println("areAllDroneEventsNull: " + areAllDroneEventsNull());
                                System.out.println("shutoff: " + shutoff);
                                if (areAllDroneEventsNull() && shutoff){
                                    allDronesShutoff = true;
                                    System.out.println("HERE");
                                }
                            }

                            String confirm = "STATE_CHANGE_OK";
                            byte[] confirmData = confirm.getBytes();
                            DatagramPacket confirmChangePacket = new DatagramPacket(confirmData, confirmData.length, packet.getAddress(), packet.getPort());
                            socket.send(confirmChangePacket);
                        }
                    }
                    // Additional message types (e.g., drone state updates) can be handled here.
                } catch (IOException | InterruptedException e) {
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
    public synchronized Event removeEvent() throws InterruptedException {
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
        List<DroneRecord> availableDrones = getAvailableDrones();

        for (DroneRecord drone : availableDrones){
            System.out.println("Available Drone: " + drone.getDroneName());
        }

        distributeEvent(event, availableDrones);

        // Small delay to ensure drone states update, can be smaller, but I kept it as 1 second to be easier for bug fixing for now
        Thread.sleep(1000);

        droneMessagesWritable = true;
        if (droneMessages.isEmpty()) {
            droneMessagesReadable = false;
        }
        notifyAll();
        return event;
    }

    private void distributeEvent(Event event, List<DroneRecord> availableDrones) {
        if (availableDrones.isEmpty()) {
            System.out.println("No available drones to assign the event. Something has gone wrong!");
            return;
        }

        String selectedDrone = null;
        Event previousEvent = null;
        boolean only1Drone = false;

        if (availableDrones.size() == 1) {
            // If there's only one available drone, assign the event to that drone
            // This means this is the drone that just asked for an Event, which means it shouldn't have one.
            // So, just assign it and return.
            selectedDrone = availableDrones.getFirst().getDroneName();
            only1Drone = true;
            if (availableDrones.getFirst().getEvent() != null) {
                System.err.println("ERROR: Did a drone just ask for an event, but it already have one? Or some other error?");
            }
        } else {
            // If multiple drones are available, find the one closest to the zone
            selectedDrone = findClosestDrone(availableDrones).getDroneName();
        }

        if (selectedDrone != null) {
            // Check if the selected drone already had an event
            if (getDroneEventByName(selectedDrone) != null) {
                previousEvent = getDroneEventByName(selectedDrone);
            }

            // Assign the new event to the selected drone either way
            String eventData;
            if (event != null) {
                eventData = convertEventToJson(event);
            } else {
                eventData = "NO_EVENT";
            }

            DatagramPacket response;
            if (only1Drone){
                response = new DatagramPacket(eventData.getBytes(), eventData.getBytes().length, getDroneAddressByName(selectedDrone), getDronePortByName(selectedDrone));
            } else {
                response = new DatagramPacket(eventData.getBytes(), eventData.getBytes().length, getListenerAddressByName(selectedDrone), getDronePortByName(selectedDrone));
            }
            try {
                setDroneEventByName(selectedDrone, event);
                sendSocket.send(response);
            } catch (IOException e) {
                System.err.println("Error sending event to drone: " + e.getMessage());
            }

            // Redistribute the previous event recursively (if there was one)
            // Be sure to exclude the drone that was just selected!
            if (!only1Drone) {
                List<DroneRecord> updatedAvailableDrones = new ArrayList<>(availableDrones);
                String finalSelectedDrone1 = selectedDrone;
                updatedAvailableDrones.removeIf(drone -> drone.getDroneName().equals(finalSelectedDrone1));

                String secondEventData;
                if (previousEvent != null) {
                    secondEventData = convertEventToJson(previousEvent);
                } else {
                    secondEventData = "NO_EVENT";
                }
                String redistributedDrone = findClosestDrone(updatedAvailableDrones).getDroneName();
                DatagramPacket secondResponse = new DatagramPacket(secondEventData.getBytes(), secondEventData.getBytes().length, getListenerAddressByName(redistributedDrone), getListenerPortByName(redistributedDrone));
                try {
                    setDroneEventByName(redistributedDrone, previousEvent);
                    socket.send(secondResponse);
                } catch (IOException e) {
                    System.err.println("Error sending event to second drone: " + e.getMessage());
                }
            }
        } else {
            System.out.println("No available drone without an event.");
        }
        System.out.println(selectedDrone + " is scheduled with event, " + event);
    }

    private List<DroneRecord> getAvailableDrones() {
        List<DroneRecord> availableDrones = new ArrayList<>();

        // Iterate over the drones list and add those in 'DroneIdle' or 'DroneEnRoute' state
        synchronized (drones) {
            for (DroneRecord drone : drones) {
                String state = drone.getState();
                if (state.equals("DroneIdle") || state.equals("DroneEnRoute")) {
                    availableDrones.add(drone);
                }
            }
        }

        return availableDrones;
    }


    public boolean confirmDroneInZone(Drone drone) {
        // For iteration 1, always return true.
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
        return new Event(java.sql.Time.valueOf(timeStr), zoneId, Event.EventType.fromString(eventType), Severity.fromString(severity));
    }

    // Helper to convert seconds to a "hh:mm:ss" string.
    private String convertSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    //Should be changed when movement and location for drones is implemented.
    //Gets the distance from the zone to the event's zone
    private double getDistanceFromZone(Vector2d dronePosition, Event event) {
        // Find the Zone corresponding to the event's zoneId
        Zone zone = null;
        for (Zone z : zones) {
            if (z.zoneID() == event.getZoneId()) {
                zone = z;
                break;
            }
        }

        // If no zone is found...
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found for zoneId: " + event.getZoneId());
        }

        // Get the center of the zone and position of drone:
        Vector2d zoneCentre = zone.centre();
        return zoneCentre.subtract(dronePosition).magnitude();
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
    public void setDroneCoordinatesByName(String droneName, int x, int y) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                drone.setXY(new int[]{x, y});
                return;
            }
        }
    }
    public InetAddress getDroneAddressByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone.getDroneAddress();
            }
        }
        return null;  // Return null if droneName is not found
    }
    public int getDronePortByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone.getDronePort();
            }
        }
        return -1;  // Return -1 if droneName is not found
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
    public boolean areAllDroneEventsNull() {
        for (DroneRecord drone : drones) {
            if (drone.getEvent() != null) {
                System.out.println("NON NULL EVENT IN: " + drone.getDroneName() + " which has event: " + drone.getEvent());
                return false;
            }
        }
        return true; // All elements were null
    }


    // Right now, closest drone determines which drone is closer to 0,0, which obviously doesn't make
    // practical sense, but since movement is being integrated, I'm making the actual calculation then
    private DroneRecord findClosestDrone(List<DroneRecord> availableDrones) {
        DroneRecord closestDrone = null;
        double minDistanceSquared = Double.MAX_VALUE;

        for (DroneRecord drone : availableDrones) {
            double x = drone.getXY()[0];
            double y = drone.getXY()[1];
            double distanceSquared = (x * x) + (y * y);

            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                closestDrone = drone;
            }
        }

        return closestDrone;
    }


}
