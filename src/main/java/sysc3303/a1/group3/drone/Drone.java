package sysc3303.a1.group3.drone;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.Map;
import java.util.Optional;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Severity;
import sysc3303.a1.group3.Zone;
import sysc3303.a1.group3.physics.Kinematics;
import sysc3303.a1.group3.physics.Vector2d;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Represents a drone that requests events via UDP.
 */
public class Drone implements Runnable {
    public static final int DRONE_LOOP_SLEEP_MS = 100; // milliseconds
    public static final int DRONE_TRAVEL_SPEEDUP = 30; // times faster than real time
    public static final boolean PRINT_DRONE_ITERATIONS = false; // Prints the drone's position every iteration
    private boolean isDroneBoundToGetStuck;
    private final String name;
    private final long startTimeMillis;
    private final ScheduledExecutorService eventMonitorScheduler = Executors.newSingleThreadScheduledExecutor();
    // Drone Components
    private final Kinematics kinematics;
    private final WaterTank waterTank;
    private final Nozzle nozzle;
    private final Map<Integer, Zone> zones;

    private final ScheduledExecutorService stateUpdateScheduler = Executors.newSingleThreadScheduledExecutor();

    // The currently assigned event
    private Optional<Event> currentEvent;

    // The current state of the drone
    private DroneState state;

    // Socket for main control flow, requesting events, elc.
    private final DatagramSocket droneSocket;

    // Socket for constantly listening for updates that can come at any time (e.g. rescheduled with new events)
    private final DatagramSocket listenerSocket;

    // Socket for exchanging state information whenever the drone changes state
    private final DatagramSocket stateSocket;

    // Socket for listening for shutoff
    private final DatagramSocket shutoffSocket;

    // Scheduler address information
    private final InetAddress schedulerAddress;
    private final int schedulerPort;

    private boolean shutoff;
    private boolean shutdownFromFault;
    private InputStream faultInputStream;
    private DroneEventParser events = new DroneEventParser();

    public Drone(String name, DroneSpecifications specifications, String schedulerAddress, int schedulerPort, Map<Integer, Zone> zones, String faultFile) {
        this.name = name;
        this.kinematics = new Kinematics(specifications.maxSpeed(), specifications.maxAcceleration());
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle(this.waterTank);
        this.zones = zones;

        this.state = new DroneIdle();
        this.currentEvent = Optional.empty();
        this.isDroneBoundToGetStuck = false;
        shutoff = false;
        shutdownFromFault = false;
        this.startTimeMillis = System.currentTimeMillis();
        this.setPosition(new Vector2d(0,0));

        if (!faultFile.isEmpty()) {
            this.faultInputStream = getClass().getClassLoader().getResourceAsStream("drone_faults.csv");
            this.events.parse(faultInputStream);
            startEventMonitor();
        }

        try {
            this.droneSocket = new DatagramSocket();
            this.listenerSocket = new DatagramSocket();
            this.stateSocket = new DatagramSocket();
            this.shutoffSocket = new DatagramSocket();

            this.schedulerAddress = InetAddress.getByName(schedulerAddress);
            this.schedulerPort = schedulerPort;
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a Drone with arbitrary specs.
     *
     * @param name the name of the drone
     */
    public Drone(String name, String schedulerAddress, int schedulerPort, Map<Integer, Zone> zones) {
        this(name, new DroneSpecifications(10, 30), schedulerAddress, schedulerPort, zones, "");
    }

    /**
     * Creates a Drone with arbitrary specs and a specific fault file
     *
     * @param name the name of the drone
     */
    public Drone(String name, String schedulerAddress, int schedulerPort, Map<Integer, Zone> zones, String faultFile) {
        this(name, new DroneSpecifications(10, 30), schedulerAddress, schedulerPort, zones, faultFile);
    }

    /**
     * Requests a new event from the scheduler.
     *
     * @return an {@link Optional} containing the new event if one is available, or an empty {@link Optional} if no event is available.
     */
    public Optional<Event> requestNewEvent() {
        try {
            String request = "DRONE_REQ_EVENT";

            // 15% chance to have some packet loss
            // If you want to test this, just increase the chance of message corruption
            Random random = new Random();
            if (random.nextDouble() < 0.15) {
                request = corruptMessage(request);
                System.out.println(name + " sent a corrupted message: " + request);
            }

            byte[] sendData = request.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
            droneSocket.send(requestPacket);

            Thread.sleep(300);

            // Wait for the scheduler's response.
            byte[] receiveData = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);
            droneSocket.receive(responsePacket);
            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());

            // Handle/parse the event
            if (response.equals("NO_EVENT")) {
                return Optional.empty();
            } else if (response.equals("NOT_RECEIVED")){
                // If the message was corrupted, loop and try again (recursion)
                return requestNewEvent();
            } else {
                byte[] respondData = "EVENT_RECEIVED".getBytes();
                DatagramPacket respondPacket = new DatagramPacket(respondData, respondData.length, responsePacket.getAddress(), responsePacket.getPort());
                droneSocket.send(respondPacket);

                return Optional.of(convertJsonToEvent(response));
            }
        } catch (IOException e) {
            System.err.println("Error requesting event: " + e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String corruptMessage(String message) {
        Random random = new Random();
        int numCharsToRemove = random.nextInt(6) + 1;

        // Note: StringBuilder objects are same as String, but have some added functionality to help me remove characters.
        // It's like it's a C string for example
        // Remove some random characters to simulate packetloss
        StringBuilder messedUpString = new StringBuilder(message);
        for (int i = 0; i < numCharsToRemove; i++) {
            if (messedUpString.length() > 1) {
                int removeIndex = random.nextInt(messedUpString.length());
                messedUpString.deleteCharAt(removeIndex);
            }
        }
        return messedUpString.toString();
    }

    public Optional<Event> checkEventUpdate() {
        byte[] receiveData = new byte[1024];
        DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
        try {
            listenerSocket.setSoTimeout(100);
            listenerSocket.receive(packet);
            String message = new String(packet.getData(), 0, packet.getLength());

            // If the message is SHUTOFF, close the startUDPListener
            if (message.equals("SHUTOFF")) {
                shutoff = true;
                System.out.println("Drone " + name + " recieved SHUTOFF message. Finishing current event and shutting down.");
                return Optional.empty();
            } else if (message.equals("NO_EVENT")) {
                return Optional.empty();

                // If none of the above, then it is an event to tell the drone to change course
            } else {
                byte[] respondData = "EVENT_RECEIVED".getBytes();
                DatagramPacket respondPacket = new DatagramPacket(respondData, respondData.length, packet.getAddress(), packet.getPort());
                listenerSocket.send(respondPacket);

                return Optional.of(convertJsonToEvent(message));
            }
        } catch (SocketTimeoutException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transitions this drone's state to a new state.
     * Sends its state to the scheduler and blocks until the scheduler confirms it received the message so they do not desync.
     *
     * @param newState the new state to transition to.
     */
    public void transitionState(DroneState newState) {
        if (newState == state) {
            System.out.println("[Error3303]: " + name + " is already in state " + state.getStateName() + " when " + newState + " was requested.");
            return;
        }
        if (PRINT_DRONE_ITERATIONS) {
            System.out.println(name + " is transitioning from " + state.getStateName() + " to " + newState.getStateName() + ".");
        }

        sendStateToScheduler(newState.getStateName());
        this.state = newState;
    }

    protected boolean isInZoneSchedulerResponse() {
        return true;
    }

    protected void fillWaterTank() {
        if (!waterTank.isFull()) {
            waterTank.fillWaterLevel();
            System.out.println(name + "'s tank filled up to full!");
        }
    }

    private void registerDroneToScheduler() {
        // First, send the scheduler it's information so it can add the drone to its records
        // Register the listener port

        Vector2d startingPosition = getPosition();
        String x = String.valueOf(startingPosition.getX());
        String y = String.valueOf(startingPosition.getY());

        System.out.println(name + " is being initialized by the scheduler with info : " + this.name + "," + this.state + "," + x + "," + y);
        String listenerRecordString = ("NEW_DRONE_LISTENER," + this.name + "," + this.state + "," + x + "," + y);
        byte[] listenerSendData = listenerRecordString.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(listenerSendData, listenerSendData.length, schedulerAddress, schedulerPort);
        try {
            listenerSocket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            listenerSocket.receive(receivePacket);
        } catch (Exception e) {
        }


        // Register the drone port (main port for asking for events)
        String droneRecordString = ("NEW_DRONE_PORT," + this.name + "," + this.state + "," + x + "," + y);
        byte[] droneSendData = droneRecordString.getBytes();
        requestPacket = new DatagramPacket(droneSendData, droneSendData.length, schedulerAddress, schedulerPort);
        try {
            droneSocket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        receiveData = new byte[1024];
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            droneSocket.receive(receivePacket);
        } catch (Exception e) {
        }

        // Finally, shutoff Port
        String shutOffRecordString = ("NEW_SHUTOFF_PORT," + this.name + "," + this.state + "," + x + "," + y);
        byte[] shutOffSendData = shutOffRecordString.getBytes();
        requestPacket = new DatagramPacket(shutOffSendData, shutOffSendData.length, schedulerAddress, schedulerPort);
        try {
            shutoffSocket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        receiveData = new byte[1024];
        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            shutoffSocket.receive(receivePacket);
        } catch (Exception e) {
        }
    }

    public void listenForShutoff() {
        new Thread(() -> {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                shutoffSocket.receive(receivePacket);
            } catch (Exception e) {
            }

            shutoff = true;
            System.out.println("Drone " + name + " recieved SHUTOFF message. Finishing current event and shutting down.");
        }).start();
    }

    @Override
    public void run() {
        registerDroneToScheduler();
        listenForShutoff();

        // send state and position every 2 seconds
        stateUpdateScheduler.scheduleAtFixedRate(() -> sendStateToScheduler(state.getStateName()), 0, 2, TimeUnit.SECONDS);

        while ((!shutdownFromFault) && ((!shutoff) || (!(state instanceof DroneIdle)))) {
            if (PRINT_DRONE_ITERATIONS) {
                System.out.printf("Drone %s, state: %s, liters: %.0f, position: %s\n",
                    name, state.getStateName(), waterTank.getWaterLevel(), kinematics.getPosition());
            }

            Optional<Event> newEvent = (currentEvent.isEmpty()) ? requestNewEvent() : checkEventUpdate();

            // Handle event
            newEvent.ifPresent(event -> state.onNewEvent(this, event));

            // Tick state
            state.runState(this);

            // Get the next state and transition if a new state was provided
            DroneState nextState = state.getNextState(this);
            if (nextState != state) {
                transitionState(nextState);
            }

            try {
                Thread.sleep(DRONE_LOOP_SLEEP_MS);
            } catch (InterruptedException e) {
                System.err.println("Drone thread interrupted: " + e.getMessage());
            }
        }

        System.out.println(Thread.currentThread().getName() + " is shutting down.");
//        droneSocket.close();
//        stateSocket.close();
//        listenerSocket.close();
        closeSockets();
        stateUpdateScheduler.shutdown();
        eventMonitorScheduler.shutdown();
    }


    // HELPERS and GETTERS/SETTERS:

    // Sends this drone's state to the scheduler.
    private void sendStateToScheduler(String stateMsg) {

        Vector2d startingPosition = getPosition();
        String x = String.valueOf(startingPosition.getX());
        String y = String.valueOf(startingPosition.getY());

        String msg = String.format("STATE_CHANGE," + name + "," + stateMsg + "," + x + "," + y);
        try {
            byte[] sendData = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
            stateSocket.send(packet);

            // Waits for a response before moving again to not desync with the scheduler's records
            byte[] confirmData = new byte[1024];
            DatagramPacket confirmPacket = new DatagramPacket(confirmData, confirmData.length);
            stateSocket.receive(confirmPacket);

        } catch (IOException e) {
            // This may run because a remaining position update sent as everything shuts down.
            // This is not a big deal if everything closes proper.
            // System.err.println("Error sending state to scheduler: " + e.getMessage());
        }
    }

    // Simple parser to convert a JSON string to an Event object.
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

    // Helper method to convert seconds to "hh:mm:ss" format.
    private String convertSecondsToTimeString(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public Vector2d getPosition() {
        return kinematics.getPosition();
    }

    // GETTERS/SETTERS
    public Optional<Event> getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(Event event) {
        currentEvent = Optional.of(event);
    }

    public void clearEvent() {
        currentEvent = Optional.empty();
    }

    public Nozzle getNozzle() {
        return nozzle;
    }

    public DroneState getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public void setPosition(Vector2d p) {
        kinematics.setPosition(p);
    }

    private void setTargetZone() {
        int zoneId = currentEvent.map(Event::getZoneId).orElseThrow(() -> new IllegalStateException("No event set on " + this));

        Zone zone = zones.get(zoneId);
        if (zone == null) {
            throw new IllegalArgumentException("Zone ID out of bounds: " + zoneId);
        }
        kinematics.setTarget(zone.centre());
    }

    public void moveToZone() {
        setTargetZone();
        kinematics.tick();
    }

    public void moveToHome() {
        kinematics.setTarget(Vector2d.ZERO);
        kinematics.tick();
    }

    public boolean isAtZone() {
        return kinematics.isAtTarget();
    }

    public boolean isAtHome() {
        return kinematics.getTarget().equals(Vector2d.ZERO) && kinematics.isAtTarget();
    }

    public boolean isStuck() {
        return isDroneBoundToGetStuck;
    }

    public void handleFault(DroneState faultState) {
        String msg;
        if(faultState instanceof DroneStuck) {
            System.out.println("Drone " + name + " is stuck. Communicating shutdown to scheduler then shutting down.");
            msg = String.format("DRONE_FAULT," + name + ",SHUTDOWN," + faultState.getStateName());
            shutoff = true;
            shutdownFromFault = true;

            try {
                byte[] sendData = msg.getBytes();
                DatagramPacket packet = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
                stateSocket.send(packet);

                // Waits for a response to confirm shutdown
                byte[] confirmData = new byte[1024];
                DatagramPacket confirmPacket = new DatagramPacket(confirmData, confirmData.length);
                stateSocket.receive(confirmPacket);

            } catch (IOException e) {
                System.err.println("Error sending state to scheduler: " + e.getMessage());
            }
        } else {
            nozzle.repairNozzle(getPosition());
        }
    }

    public void closeSockets() {
        if (droneSocket != null && !droneSocket.isClosed()) {
            droneSocket.close();
        }
        if (listenerSocket != null && !listenerSocket.isClosed()) {
            listenerSocket.close();
        }
        if (stateSocket != null && !stateSocket.isClosed()) {
            stateSocket.close();
        }
    }

    private void startEventMonitor() {
        eventMonitorScheduler.scheduleAtFixedRate(() -> {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            long simulatedSeconds = elapsedMillis / (1000);
            String timeStr = convertSecondsToTimeString((int) simulatedSeconds);
            Time currentSimTime = Time.valueOf(timeStr);

            for (DroneEvent event : events.getEvents()) {
                if (event.getTime().toString().equals(currentSimTime.toString())
                    && name.equals(event.getDroneId())) {

                    System.out.println(">>> EVENT TRIGGERED for " + name + " at " + currentSimTime + ": " + event.getEventType());


                    if (event.getEventType().equals("NOZZLE_JAM")) {
                        //Implement Nozzle Stuck instructions
                        nozzle.nozzleStuck();
                    } else if (event.getEventType().equals("DRONE_STUCK")) {
                        isDroneBoundToGetStuck = true;
                        // Drone Struck instructions (
                    }
                }
            }

        }, 0, 1, TimeUnit.SECONDS);
    }




}
