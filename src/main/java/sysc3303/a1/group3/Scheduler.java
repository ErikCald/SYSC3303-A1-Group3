package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.*;
import sysc3303.a1.group3.physics.Vector2d;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {

    private final int MAX_SIZE = 10;

    private final ScheduledExecutorService uiUpdater = Executors.newSingleThreadScheduledExecutor();

    // Queue for events coming from the subsystem.
    private final Queue<Event> droneMessages = new ArrayDeque<>();
    private boolean droneMessagesWritable = true;
    private boolean droneMessagesReadable = false;

    // Drone List is now a List of DroneRecords to hold info about drones the scheulder has contact to
    private final List<DroneRecord> drones = Collections.synchronizedList(new ArrayList<>());

    private final List<Zone> zones;

    // Boolean for if the system should shut down
    private volatile boolean shutoff = false;
    private volatile boolean attemptShutoff = false;

    // Boolean for if all drones have no events, only to be set after shutdown is true.
    // Ensures that the scheduler doesn't shut down while the drone still needs to send packets to it.
    private volatile boolean allDronesShutoff = false;

    // general purpose socket
    private final DatagramSocket socket;

    // socket to exclusive send packets when the main socket is busy listening
    private final DatagramSocket sendSocket;

    // UI to display where the zones and drones are
    private final UI ui;

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

        this.ui = new UI(zones);

        // start the main listener/handler
        startUDPListener();
        startUI();
    }

    private void startUI() {
        uiUpdater.scheduleAtFixedRate(() -> {

            // UPDATE UI STUFF HERE

            // e.g. update the JFrame ...



            // NOTE: sometimes you may see that the drones have a negative number for position when returning. I think this is because the drones move really quick
            // (the simulation is sped up by x10), so they sometimes "overshoot" their location, but they eventually correct for this and arrive at their location.

            // If Drones are less than 0,0 maybe just display at 0.
            // Use ctrl+shift+f and enter "ALLOWABLE_ERROR" if you want to change how close the drone has to be to the
            // center of the target to count as there (I set it to 10 rn), this should make it easier for the UI to be more percise, but sometimes the drone will
            // just fly around the zone to correct itself for a bit. Feel free to make ALLOWABLE_ERROR larger if you want to mitigate the drones correcting themselves,
            // but this may make it harder for the UI to be precise as drones may be further from the center of the zone.
            // Alternatively, you can make it so the drone kinematics ticket more frequently, giving a more accurate position


            // Debug print below to show you what the drone data looks like for updating stuffs
            // Obviously, delete this later, this is just to show you how to access the simulation data
            // You should probably decrease the polling timer (e.g. to 100ms), I have it on 1000ms rn because I'm printing stuff for y'all to see.
            System.err.println("\n\n=== Drone Status ===");
            synchronized (drones) {
                ui.updateDrones(drones);
                
                for (DroneRecord drone : drones) {
                    System.err.println("Drone: " + drone.getDroneName() +
                        ", State: " + drone.getState() +
                        ", Event: " + (drone.getEvent() != null ? drone.getEvent() : "None") +
                        ", Position: " + drone.getPosition());
                }
            }
            System.err.println("--------------------");

            // Determine active fires by checking drone state and event type.
            // NOTE: Drones that are returning are ignored since the fire is extinguished
            Set<Event> activeFires = new HashSet<>();
            for (DroneRecord drone : drones) {
                if ((!drone.getState().equals("DroneReturning")) && (drone.getEvent() != null)) {
                    activeFires.add(drone.getEvent());
                }
            }
            
            // Print active fires
            System.err.println("\n===== Active Fires =====");
            List<Integer> zoneFires = new ArrayList<>();
            if (!activeFires.isEmpty()) {
                for (Event fire : activeFires) {
                    System.err.printf("Zone: %d | Type: %s | Severity: %s%n",
                    fire.getZoneId(), fire.getEventType(), fire.getSeverity());
                    zoneFires.add(fire.getZoneId());
                }
            }
            ui.updateFireStates(zoneFires);
            System.err.println("---------------------------------\n");

        }, 0, 200, TimeUnit.MILLISECONDS);
    }


    // Stops UI scheduler object.
    // Put any UI stuffs to be shutdown in here too
    public void stopUI() {
        System.out.println("Shutting down UI...");
        uiUpdater.shutdown();
        try {
            // Force shutdown if not terminated in time
            if (!uiUpdater.awaitTermination(1, TimeUnit.SECONDS)) {
                uiUpdater.shutdownNow();
            }
        } catch (InterruptedException e) {
            uiUpdater.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close UI components:
        // e.g. close frame ...


    }


    // Starts one UDP listener thread that continuously receives packets.
    // Based on the packet info, respond and react accordingly
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
                        handleSubsystemEvent(message);
                    } else if (message.startsWith("DRONE_REQ_EVENT")) {
                        // Received a request from a drone.
                        System.out.println("Scheduler received a request for an event from " + getDroneByPort(packet.getPort()).getDroneName());
                        handleDroneRequest(packet);
                    } else if (message.equals("SHUTDOWN")) {
                        // Received a shutdown request from FiSubsystem.
                        handleShutdown();
                    } else if (message.startsWith("NEW_DRONE_LISTENER")) {
                        // Adds new DroneRecord: for listenerSocket
                        handleNewDroneListener(message, packet);
                    } else if (message.startsWith("NEW_DRONE_PORT")){
                        // Adds new DroneRecord: for droneSocket
                        handleNewDronePort(message, packet);
                    } else if (message.startsWith("NEW_SHUTOFF_PORT")){
                        // Adds new DroneRecord: for shutoffSocket
                        handleNewShutoffPort(message, packet);
                    } else if (message.startsWith("STATE_CHANGE")) {
                        // Handles drone state change updates
                        handleStateChange(message, packet);
                    } else if (message.startsWith("DRONE_FAULT")) {
                        // Handle drone shutdown with fault
                        handleDroneFault(message, packet);
                    } else {
                        corruptedMessage(packet);
                        // Additional message types (e.g., drone state updates) can be handled here.
                    }
                } catch (IOException e) {
                    break;
                }
            }
            socket.close();
            sendSocket.close();
            stopUI();

            System.out.println("Scheduler's UDP Listener is closing.");
        }, "Scheduler-UPDListener").start();
    }

    // UDP HANDLER FUNCTIONS:

    private void handleSubsystemEvent(String message) {
        new Thread(() -> {
            String json = message.substring("SUBSYSTEM_EVENT:".length());
            Event event = convertJsonToEvent(json);
            addEvent(event);
        }).start();
    }
    private void handleDroneRequest(DatagramPacket packet) {
        new Thread(() -> {
            try {
                removeEvent(packet.getAddress(), packet.getPort());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "Scheduler-RemoveEvent").start();
    }
    private void handleShutdown() {
        new Thread(() -> {
            try {
                shutOff();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
    private void handleDroneFault(String message, DatagramPacket packet) {
        new Thread(() -> {
            synchronized (drones) {
                String[] parts = message.split(",");
                String name = parts[1];
                boolean isShutdown = parts[2].equals("SHUTDOWN");
                String faultState = parts[3];
                String prevState = parts[4];

                // Reclaim incomplete event, if fire has not been put out yet
                // Note, we do not reclaim an event if InZone or Extinguishing as the fire should be put out already
                if (Objects.equals(prevState, "DroneIdle") || Objects.equals(prevState, "DroneEnRoute")){
                    System.out.println("Stuck Drone, " + name + " has STATE: " + prevState + ". Reclaiming event.");
                    Event droneEvent = getDroneEventByName(name);
                    if (droneEvent != null) {
                        addBackEvent(droneEvent);
                    }
                }

                if(isShutdown) {
                    System.out.println("Drone " + name + " has fault: " + faultState + ". Reclaiming event and removing from drone scheduling.");
                    drones.removeIf(drone -> drone.getDroneName().equals(name));
                } else {
                    System.out.println("Drone " + name + " has fault: " + faultState + ". Reclaiming event.");
                }

                sendConfirmation(packet, "FAULT_RECIEVED");
            }
        }).start();
    }
    private void handleNewDroneListener(String message, DatagramPacket packet) {
        new Thread(() -> {
            synchronized (drones) {
                String[] parts = message.split(",");
                String name = parts[1];
                String state = parts[2];
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);

                DroneRecord drone = getDroneByName(name);
                if (drone == null) {
                    drone = new DroneRecord(name, state, x, y);
                    drones.add(drone);
                }
                drone.setListenerAddress(packet.getAddress());
                drone.setListenerPort(packet.getPort());
            }

            sendConfirmation(packet, "LISTENER_OK");
        }).start();
    }
    private void handleNewDronePort(String message, DatagramPacket packet) {
        new Thread(() -> {
            synchronized (drones) {
                String[] parts = message.split(",");
                String name = parts[1];
                String state = parts[2];
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);

                DroneRecord drone = getDroneByName(name);
                if (drone == null) {
                    drone = new DroneRecord(name, state, x, y);
                    drones.add(drone);
                }
                drone.setDroneAddress(packet.getAddress());
                drone.setDronePort(packet.getPort());
            }

            sendConfirmation(packet, "DRONE_OK");
        }).start();
    }
    private void handleNewShutoffPort(String message, DatagramPacket packet) {
        new Thread(() -> {
            synchronized (drones) {
                String[] parts = message.split(",");
                String name = parts[1];
                String state = parts[2];
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);

                DroneRecord drone = getDroneByName(name);
                if (drone == null) {
                    drone = new DroneRecord(name, state, x, y);
                    drones.add(drone);
                }
                drone.setShutOffAddress(packet.getAddress());
                drone.setShutOffPort(packet.getPort());
            }

            sendConfirmation(packet, "DRONE_OK");
        }).start();
    }
    private void handleStateChange(String message, DatagramPacket packet) {
        synchronized (drones) {
            String[] parts = message.split(",");
            String name = parts[1];
            String state = parts[2];
            double x = Double.parseDouble(parts[3]);
            double y = Double.parseDouble(parts[4]);

            setDroneStateByName(name, state);
            setDronePositionByName(name, x, y);
            if (Objects.equals(state, "DroneIdle")) {
                getDroneByName(name).setEvent(null);

                if (areAllDroneEventsNull() && attemptShutoff) {
                    try {
                        shutOff();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            sendConfirmation(packet, "STATE_CHANGE_OK");
        }
    }
    private void corruptedMessage(DatagramPacket packet) {
        synchronized (socket) {
            System.out.println("Scheduler got a corrupted message, asking for drone to resend...");
            byte[] askAgain = "NOT_RECEIVED".getBytes();
            DatagramPacket confirmPacket = new DatagramPacket(askAgain, askAgain.length, packet.getAddress(), packet.getPort());
            try {
                socket.send(confirmPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    // NOTE: THIS FUNCTION USES THE DEFAULT "socket", NOT "sendSocket"
    // Be careful if you want to use this in other functions
    private void sendConfirmation(DatagramPacket packet, String confirmationMessage) {
        synchronized (socket) {
            byte[] confirmData = confirmationMessage.getBytes();
            DatagramPacket confirmPacket = new DatagramPacket(confirmData, confirmData.length, packet.getAddress(), packet.getPort());
            try {
                socket.send(confirmPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



    // FUNCTIONS FOR ADDING AND REMOVING/DISTRIBUTING EVENTS:

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

    public synchronized void addBackEvent(Event event) {
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
        Thread.sleep(100);

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

        String selectedDroneName;
        String originalDroneName = getDroneByPort(originalPort).getDroneName();
        Event previousEvent;
        String eventData;
        if (event != null) {
            eventData = convertEventToJson(event);
        } else {
            eventData = "NO_EVENT";
        }

        if (availableDrones.size() == 1) {
            if (getDroneByName(originalDroneName).getEvent() != null) {
                System.err.println("ERROR: Did a drone just ask for an event, but it already have one? Or some other error?");
            } else {
                //send packet to the drone:
                sendEventToDrone(getDroneByName(originalDroneName), event, sendSocket, originalAddress, originalPort);
            }
        } else {
            // If multiple drones are available, find the one closest to the zone
            selectedDroneName = findClosestDrone(availableDrones, event).getDroneName();
            previousEvent = getDroneEventByName(selectedDroneName);

            if (selectedDroneName != null && ifNewEventFurther(event, previousEvent)) {
                // if the drone already had an event, redistribute

                // First, send the original drone the redistributed event
                previousEvent = getDroneEventByName(selectedDroneName);
                sendEventToDrone(getDroneByPort(originalPort), previousEvent, sendSocket, originalAddress, originalPort);
                System.out.println(originalDroneName + " is re-scheduled with older event, " + previousEvent);

                // Next, send the newest event to the closer drone:
                sendEventToDrone(getDroneByName(selectedDroneName), event, sendSocket, getListenerAddressByName(selectedDroneName), getListenerPortByName(selectedDroneName));
                System.out.println(selectedDroneName + " is scheduled with newer event, " + event + "\n");

                return;
            }
            // Default case, just send the packet to the drone:
            sendEventToDrone(getDroneByPort(originalPort), event, sendSocket, originalAddress, originalPort);
        }
    }
    private void sendEventToDrone(DroneRecord drone, Event event, DatagramSocket socket, InetAddress address, int port) {
        new Thread(() -> {
            synchronized (socket){
                try {
                    byte[] eventData = convertEventToJson(event).getBytes();
                    DatagramPacket response = new DatagramPacket(eventData, eventData.length, address, port);
                    socket.send(response);

                    byte[] receiveData = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(responsePacket);

                    //CHECK IF RESPONSE VALID? up to whoever is dealing with packet loss faults ig

                    setDroneEventByName(drone.getDroneName(), event);
                    System.out.println(drone.getDroneName() + " is scheduled with event: " + event + "\n");
                } catch (IOException e) {
                    System.err.println("Error sending event to drone: " + e.getMessage());
                }
            }
        }).start();
    }




    //UTILITY AND MORE COMPLICATED HELPER FUNCTIONS:

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

    // Synchronized shutdown method using wait/notifyAll.
    public synchronized void shutOff() throws InterruptedException {
        attemptShutoff = true;

        if (!droneMessages.isEmpty() || !areAllDroneEventsNull()) {
            //do nothing
        } else {
            this.shutoff = true;
            notifyAll();
            sendShutoffToDrones();
        }
    }

    // Sends "SHUTOFF" to all drones
    private void sendShutoffToDrones() throws InterruptedException {
        String message = "SHUTOFF";
        byte[] sendData = message.getBytes();

        for (DroneRecord drone : drones) {
            try {
                InetAddress droneAddress = drone.getShutOffAddress();
                int dronePort = drone.getShutOffPort();
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



    // GETTERS, SETTERS, AND SIMPLE HELPERS:

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
    public void setDronePositionByName(String droneName, double x, double y) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                drone.setPosition(Vector2d.of(x, y));
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
        return null;
    }
    public int getListenerPortByName(String droneName) {
        for (DroneRecord drone : drones) {
            if (drone.getDroneName().equals(droneName)) {
                return drone.getListenerPort();
            }
        }
        return -1;
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
        return true;
    }
    public Queue<Event> getDroneMessages() {
        return droneMessages;
    }
    private DroneRecord findClosestDrone(List<DroneRecord> availableDrones, Event event) {
        if (availableDrones.isEmpty()) {
            return null;
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

    // Is newEvent's zone further away than olderEvent's zone?
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

        double newEventDistance = newEventZone.centre().magnitude();
        double olderEventDistance = olderEventZone.centre().magnitude();

        return newEventDistance > olderEventDistance;
    }

    public boolean confirmDroneInZone(Drone drone) {
        // For current iteration when errors cannot happen yet, always return true.
        return true;
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
