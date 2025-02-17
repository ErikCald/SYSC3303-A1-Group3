package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;

public class Drone implements Runnable {

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

    public Drone(Scheduler scheduler) {
        this.sensors = new Sensors();
        this.motors = new Motors();
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle();
        this.scheduler = scheduler;
        this.state = STATES.retrieve(DroneIdle.class);

        droneCounter++;
        this.positionX = droneCounter;
        this.positionY = droneCounter;
    }

    /**
     * Transitions from the current state to a new state.
     * {@link DroneState#triggerExitWork(Drone)} is invoked on the current state before the transition,
     * after which {@link DroneState#triggerEntryWork(Drone)} is invoked on the new state.
     *
     * @param state the new state to transition to
     */
    void transitionState(Class<? extends DroneState> state) throws InterruptedException {
        this.state.triggerExitWork(this);
        this.state = STATES.retrieve(state);
        this.state.triggerEntryWork(this);
    }

    //Start the Drone, wait for notifications.
    @Override
    public void run() {
        while (!scheduler.getShutOff()) {
            requestEvent();
            System.out.println(Thread.currentThread().getName() + " has been scheduled with event: \n" + currentEvent);
            System.out.println("Sending back confirmation to Fire Incident Subsystem.\n");

            if (currentEvent != null){
                scheduler.confirmWithSubsystem(currentEvent);

                //simulate drone switching to enroute, and taking off
                try {
                    transitionState(DroneEnRoute.class);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.out.println(Thread.currentThread().getName() + " is shutting down.");
    }

    // Note for Zane: If you are implementing movement, you should chance this to the actual x and y of the Drone.
    public int[] getPosition() {
        return new int[]{positionX, positionY};
    }
    public void requestEvent() {
        currentEvent = scheduler.removeEvent();
    }
    public Event getCurrentEvent(){ return currentEvent; }
    public void setCurrentEvent(Event event){ currentEvent = event;}
    public DroneState getState() { return state; }

    /**
     * @return the Scheduler that owns this Drone
     */
    Scheduler getScheduler() {
        return scheduler;
    }

}
