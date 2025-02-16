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

    public Drone(Scheduler scheduler) {
        this.sensors = new Sensors();
        this.motors = new Motors();
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle();
        this.scheduler = scheduler;
        this.state = STATES.retrieve(DroneIdle.class);
    }

    /**
     * Transitions from the current state to a new state.
     * {@link DroneState#triggerExitWork(Drone)} is invoked on the current state before the transition,
     * after which {@link DroneState#triggerEntryWork(Drone)} is invoked on the new state.
     *
     * @param state the new state to transition to
     */
    void transitionState(Class<? extends DroneState> state) {
        this.state.triggerExitWork(this);
        this.state = STATES.retrieve(state);
        this.state.triggerEntryWork(this);
    }

    //Start the Drone, wait for notifications.
    @Override
    public void run() {
        while (!scheduler.getShutOff()) {
            requestEvent();
            System.out.println("Drone has been scheduled with event: \n" + currentEvent);
            System.out.println("Sending back confirmation to Fire Incident Subsystem.\n");
            if (currentEvent != null){
                scheduler.confirmWithSubsystem(currentEvent);
            }
        }
        System.out.println(Thread.currentThread().getName() + " is shutting down.");
    }

    // package private for testing purposes
    public void requestEvent() {
        currentEvent = scheduler.removeEvent();
    }
    public DroneState getState() { return state; }

    /**
     * @return the Scheduler that owns this Drone
     */
    Scheduler getScheduler() {
        return scheduler;
    }

}
