package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Severity;
import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.physics.Kinematics;
import sysc3303.a1.group3.physics.Vector2d;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;

import java.time.Instant;

/**
 * Represents a drone that requests events via UDP.
 */
public class Drone implements Runnable {

    private static final DroneStates STATES = DroneStates.withDefaults();

    private final String name;
    private final Scheduler scheduler;

    private final Kinematics kinematics;
    private final WaterTank waterTank;
    private final Nozzle nozzle;


    // The currently assigned event.
    Event currentEvent;

    private DroneState state;

    private double lastTickTimeMillis;

    private DatagramSocket droneSocket;
    private DatagramSocket listenerSocket;
    private DatagramSocket stateSocket;

    private InetAddress schedulerAddress;
    private int schedulerPort;

    private volatile boolean shutoff;

    public Drone(String name, DroneSpecifications specifications, Scheduler scheduler, String schedulerAddress, int schedulerPort) {
        this.name = name;

        this.kinematics = new Kinematics(specifications.maxSpeed(), specifications.maxAcceleration());
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle(this.waterTank);

        this.scheduler = scheduler;
        this.state = STATES.retrieve(DroneIdle.class);

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
    public Drone(String name, Scheduler scheduler, String schedulerAddress, int schedulerPort) {
        this(name, new DroneSpecifications(10, 30), scheduler, schedulerAddress, schedulerPort);
    }

    private void startUDPListener() {
        new Thread(() -> {
            byte[] receiveData = new byte[1024];
            while (!shutoff) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                try {
                    listenerSocket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());
                    if (message.equals("SHUTOFF")) {
                        this.shutoff();
                    } else if (message.equals("NO_EVENT")) {
                        currentEvent = null;
                    } else {
                        currentEvent = convertJsonToEvent(message);
                    }
                } catch (IOException e) {

                }
            }
            listenerSocket.close();
        }).start();
    }

    // The drone sends a "DRONE_REQ_EVENT" packet and waits for the scheduler's response.
    public void requestEvent() {
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
            if (response.equals("NO_EVENT")) {
                currentEvent = null;
            } else {
                currentEvent = convertJsonToEvent(response);
            }
        } catch (IOException e) {
            System.err.println("Error requesting event: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Transitions this drone's state.
    public void transitionState(Class<? extends DroneState> newState) throws InterruptedException {
        if (Objects.equals(this.state.getClass(), newState)) {
            return;
        }

        this.state.triggerExitWork(this);
        sendStateToScheduler(newState.getSimpleName());
        this.state = STATES.retrieve(newState);
        this.state.triggerEntryWork(this);
    }

    protected boolean InZoneSchedulerResponse(){
        return (this.scheduler.confirmDroneInZone(this));
    }

    protected void extinguishFlames() throws InterruptedException {
        System.out.println(name + " is extinguishing flames!");
        nozzle.extinguish();
    }

    @Override
    public void run() {
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
        String droneRecordString = ("NEW_DRONE_PORT," + this.name + "," + this.state + "," + positionX + "," + positionY);
        byte[] droneSendData = droneRecordString.getBytes();
        requestPacket = new DatagramPacket(droneSendData, droneSendData.length, schedulerAddress, schedulerPort);
        try {
            droneSocket.send(requestPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        startUDPListener();

        while (!shutoff) {
            requestEvent();
            if (currentEvent != null) {
                try {
                    transitionState(DroneEnRoute.class);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
    private void shutoff(){
        this.shutoff = true;
    }
    public Event getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(Event event) { currentEvent = event; }
    public DroneState getState() { return state; }
    public String getName() { return name; }
    Scheduler getScheduler() { return scheduler; }
}
