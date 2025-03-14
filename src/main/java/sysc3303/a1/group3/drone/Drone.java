package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Severity;
import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Represents a drone that requests events via UDP.
 */
public class Drone implements Runnable {

    private final String name;

    private final Sensors sensors;
    private final Motors motors;
    private final WaterTank waterTank;
    private final Nozzle nozzle;

    private static final DroneStates STATES = DroneStates.withDefaults();

    private final Scheduler scheduler;
    // The currently assigned event.
    Event currentEvent;

    private DroneState state;

    // For testing: assign positions via a static counter.
    private static int droneCounter = 0;
    private final int positionX;
    private final int positionY;

    private DatagramSocket droneSocket;
    private InetAddress schedulerAddress;
    private int schedulerPort;

    public Drone(String name, Scheduler scheduler, String schedulerAddress, int schedulerPort) {
        this.name = name;
        this.sensors = new Sensors();
        this.motors = new Motors();
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle(this.waterTank);
        this.scheduler = scheduler;
        this.state = STATES.retrieve(DroneIdle.class);

        droneCounter++;
        this.positionX = droneCounter;
        this.positionY = droneCounter;

        try {
            this.droneSocket = new DatagramSocket();
            this.schedulerAddress = InetAddress.getByName(schedulerAddress);
            this.schedulerPort = schedulerPort;
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String getStateAsJson() {
        return String.format("{\"name\":\"%s\", \"state\":\"%s\", \"x\":%d, \"y\":%d}",
            name, state.getStateName(), positionX, positionY);
    }

    // Sends this drone's state to the scheduler.
    private void sendStateToScheduler() {
        try {
            String stateData = getStateAsJson();
            byte[] sendData = stateData.getBytes();
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
            droneSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Error sending state to scheduler: " + e.getMessage());
        }
    }

    // The drone sends a "DRONE_REQ_EVENT" packet and waits for the scheduler's response.
    public void requestEvent() {
        try {
            String request = "DRONE_REQ_EVENT";
            byte[] sendData = request.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(sendData, sendData.length, schedulerAddress, schedulerPort);
            droneSocket.send(requestPacket);

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

    // Transitions this drone's state.
    public void transitionState(Class<? extends DroneState> newState) throws InterruptedException {
        if (Objects.equals(this.state.getClass(), newState)) {
            return;
        }
        this.state.triggerExitWork(this);
        this.state = STATES.retrieve(newState);
        this.state.triggerEntryWork(this);
        sendStateToScheduler();
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
        while (!scheduler.getShutOff()) {
            requestEvent();
            if (currentEvent != null) {
                try {
                    transitionState(DroneEnRoute.class);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            sendStateToScheduler();
        }
        System.out.println(Thread.currentThread().getName() + " is shutting down.");
    }

    public int[] getPosition() {
        return new int[]{positionX, positionY};
    }
    public Event getCurrentEvent() { return currentEvent; }
    public void setCurrentEvent(Event event) { currentEvent = event; }
    public DroneState getState() { return state; }
    public String getName() { return name; }
    Scheduler getScheduler() { return scheduler; }
}
