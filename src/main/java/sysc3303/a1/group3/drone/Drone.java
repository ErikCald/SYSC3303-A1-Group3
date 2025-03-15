package sysc3303.a1.group3.drone;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.Severity;
import sysc3303.a1.group3.Zone;
import sysc3303.a1.group3.physics.Kinematics;
import sysc3303.a1.group3.physics.Vector2d;

/**
 * Represents a drone that requests events via UDP.
 */
public class Drone implements Runnable {
    public static final int DRONE_LOOP_SLEEP_MS = 100; // milliseconds
    public static final int DRONE_TRAVEL_SPEEDUP = 30; // times faster than real time
    public static final boolean PRINT_DRONE_ITERATIONS = false; // Prints the drone's position every iteration

    private final String name;
    private final Scheduler scheduler;

    // Drone Components
    private final Kinematics kinematics;
    private final WaterTank waterTank;
    private final Nozzle nozzle;
    private final List<Zone> zones;

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

    // Scheduler address information
    private final InetAddress schedulerAddress;
    private final int schedulerPort;

    private boolean shutoff;

    public Drone(String name, DroneSpecifications specifications, Scheduler scheduler, String schedulerAddress, int schedulerPort, List<Zone> zones) {
        this.name = name;

        this.kinematics = new Kinematics(specifications.maxSpeed(), specifications.maxAcceleration());
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle(this.waterTank);
        this.zones = zones;

        this.scheduler = scheduler;
        this.state = new DroneIdle();

        shutoff = false;

        try {
            this.droneSocket = new DatagramSocket();
            this.listenerSocket = new DatagramSocket();
            this.stateSocket = new DatagramSocket();

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
     * @param scheduler the scheduler that is responsible for this Drone
     */
    public Drone(String name, Scheduler scheduler, String schedulerAddress, int schedulerPort, List<Zone> zones) {
        this(name, new DroneSpecifications(10, 30), scheduler, schedulerAddress, schedulerPort, zones);
    }

    /**
     * Requests a new event from the scheduler.
     * 
     * @return an {@link Optional} containing the new event if one is available, or an empty {@link Optional} if no event is available.
     */
    public Optional<Event> requestNewEvent() {
        try {
            String request = "DRONE_REQ_EVENT";
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
            } else {
                return Optional.of(convertJsonToEvent(response));
            }
        } catch (IOException e) {
            System.err.println("Error requesting event: " + e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    protected boolean isInZoneSchedulerResponse(){
        return (this.scheduler.confirmDroneInZone(this));
    }

    protected void fillWaterTank(){
        if(!waterTank.isFull()) {
            waterTank.fillWaterLevel();
            System.out.println(name + "'s tank filled up to full!");
        }
    }

    private void registerDroneToScheduler(){
        // First, send the scheduler it's information so it can add the drone to its records
        // Register the listener port
        double positionX = getPosition().getX();
        double positionY = getPosition().getY();
        String listenerRecordString = ("NEW_DRONE_LISTENER," + this.name + "," + this.state + "," + positionX + "," + positionY);
        byte[] listenerSendData = listenerRecordString.getBytes();
        DatagramPacket requestPacket = new DatagramPacket(listenerSendData, listenerSendData.length, schedulerAddress, schedulerPort);
        try {
            listenerSocket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Register the drone port (main port for asking for events)
        String droneRecordString = ("NEW_DRONE_PORT," + this.name + "," + this.state + "," + positionX + "," + positionY);
        byte[] droneSendData = droneRecordString.getBytes();
        requestPacket = new DatagramPacket(droneSendData, droneSendData.length, schedulerAddress, schedulerPort);
        try {
            droneSocket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        registerDroneToScheduler();
        
        while ((!shutoff) || (!(state instanceof DroneIdle))) {
            if (PRINT_DRONE_ITERATIONS) {
                System.out.printf("Drone %s, state: %s, liters: %.0f, position: %s\n",
                        name, state.getStateName(), waterTank.getWaterLevel(), kinematics.getPosition());
            }

            Optional<Event> newEvent = (currentEvent == null) ? requestNewEvent() : checkEventUpdate();
            if(newEvent.isPresent()){
                state.onNewEvent(this, newEvent.get());
            }

            state.runState(this);
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
        droneSocket.close();
        stateSocket.close();
    }




    // HELPERS and GETTERS/SETTERS:

    // Sends this drone's state to the scheduler.
    private void sendStateToScheduler(String stateMsg) {
        String msg = String.format("STATE_CHANGE," + name + "," + stateMsg);
        try {
            byte[] sendData = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
            stateSocket.send(packet);

            // Waits for a response before moving again to not desync with the scheduler's records
            byte[] confirmData = new byte[1024];
            DatagramPacket confirmPacket = new DatagramPacket(confirmData, confirmData.length);
            stateSocket.receive(confirmPacket);

        } catch (IOException e) {
            System.err.println("Error sending state to scheduler: " + e.getMessage());
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
    public Optional<Event> getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(Event event) { currentEvent = Optional.of(event); }
    public Nozzle getNozzle() { return nozzle; }
    public DroneState getState() { return state; }
    public String getName() { return name; }
    Scheduler getScheduler() { return scheduler; }

    public boolean isTankEmpty() {
        return waterTank.waterLevelEmpty();
    }

    private void setTargetZone() {
        int zoneId = currentEvent.get().getZoneId();
        if (zoneId < 0 || zoneId >= zones.size()) {
            throw new IllegalArgumentException("Zone ID out of bounds: " + zoneId);
        }

        kinematics.setTarget(zones.get(zoneId).centre());
    }

    public void moveToZone() {
        setTargetZone();
        kinematics.tick();
    }
    
    public void moveToHome() {
        kinematics.setTarget(Vector2d.of(0, 0));
        kinematics.tick();
    }

    public boolean isAtZone() {
        return kinematics.isAtTarget();
    }

    public boolean isAtHome() {
        return kinematics.getTarget().equals(Vector2d.of(0, 0)) && kinematics.isAtTarget();
    }
}
