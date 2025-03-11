package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;


public class Drone implements Runnable {

    private final String name;

    private final Sensors sensors;
    private final Motors motors;
    private final WaterTank waterTank;
    private final Nozzle nozzle;

    private static final DroneStates STATES = DroneStates.withDefaults();;

    private final Scheduler scheduler;

    // package private for testing purposes
    Event currentEvent;

    private DroneState state;

    // Static counter to assign positions. This is just for testing before we get movement and actual positions
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

    // NEW METHOD: Serialize state to JSON
    private String getStateAsJson() {
        return String.format("{\"name\":\"%s\", \"state\":\"%s\", \"x\":%d, \"y\":%d}",
            name, state.getStateName(), positionX, positionY);
    }


    // NEW METHOD: Send state to the scheduler
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


    // NEW METHOD: Receive assignment from the scheduler
    private void receiveAssignment() {
        try {
            byte[] receiveData = new byte[1024];
            DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
            droneSocket.receive(packet);
            String assignmentData = new String(packet.getData(), 0, packet.getLength());

            Event event = convertJsonToEvent(assignmentData); // Implement this method
            setCurrentEvent(event); // set current event

        } catch (IOException e) {
            System.err.println("Error receiving assignments: " + e.getMessage());
        }
    }
    private Event convertJsonToEvent(String assignmentData) {
        // Implement JSON deserialization here (e.g., using Gson library)
        return null;
    }





    /**
     * Transitions from the current state to a new state.
     * {@link DroneState#triggerExitWork(Drone)} is invoked on the current state before the transition,
     * after which {@link DroneState#triggerEntryWork(Drone)} is invoked on the new state.
     *
     * @param state the new state to transition to
     */
    public void transitionState(Class<? extends DroneState> state) throws InterruptedException {
        if (Objects.equals(this.state.getClass(), state)) {
            return;
        }
        this.state.triggerExitWork(this);
        this.state = STATES.retrieve(state);
        this.state.triggerEntryWork(this);
        sendStateToScheduler();
    }

    //Start the Drone, wait for notifications.
    @Override
    public void run() {
        receiveAssignment();
        while (!scheduler.getShutOff()) {
            requestEvent();


            if (currentEvent != null){
                //System.out.println(name + " has been scheduled with event: \n" + currentEvent);
                //System.out.println("Sending back confirmation to Fire Incident Subsystem.\n");
                //scheduler.confirmWithSubsystem(currentEvent);

                //simulate drone switching to enroute, and taking off
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

    protected boolean InZoneSchedulerResponse(){
        return (this.scheduler.confirmDroneInZone(this));
    }

    protected void extinguishFlames() throws InterruptedException {
        System.out.println(name + " is extinguishing flames!");
        nozzle.extinguish();
    }

    // Note for Zane: If you are implementing movement, you should chance this to the actual x and y of the Drone.
    public int[] getPosition() {
        return new int[]{positionX, positionY};
    }
    public void requestEvent() {
        scheduler.removeEvent();
    }
    public Thread getCurrentThread(){ return Thread.currentThread(); }
    public Event getCurrentEvent(){ return currentEvent; }
    public void setCurrentEvent(Event event){ currentEvent = event;}
    public DroneState getState() { return state; }
    public String getName(){ return name; }


    /**
     * @return the Scheduler that owns this Drone
     */
    Scheduler getScheduler() {
        return scheduler;
    }

}
