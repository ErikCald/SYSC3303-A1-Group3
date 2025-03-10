package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.physics.Kinematics;
import sysc3303.a1.group3.physics.Vector2d;

import java.time.Instant;

public class Drone implements Runnable {

    private static final DroneStates STATES = DroneStates.withDefaults();

    private final String name;
    private final Scheduler scheduler;

    private final Kinematics kinematics;
    private final WaterTank waterTank;
    private final Nozzle nozzle;

    // package private for testing purposes
    Event currentEvent;

    private DroneState state;

    private double lastTickTimeMillis;

    /**
     * Creates a new Drone.
     *
     * @param name the name of the drone
     * @param scheduler the scheduler that is responsible for this Drone
     * @param specifications the physical specifications of this Drone
     */
    public Drone(String name, Scheduler scheduler, DroneSpecifications specifications) {
        this.name = name;

        this.kinematics = new Kinematics(specifications.maxSpeed(), specifications.maxAcceleration());
        this.waterTank = new WaterTank();
        this.nozzle = new Nozzle(this.waterTank);

        this.scheduler = scheduler;
        this.state = STATES.retrieve(DroneIdle.class);
    }

    /**
     * Creates a Drone with arbitrary specs.
     *
     * @param name the name of the drone
     * @param scheduler the scheduler that is responsible for this Drone
     */
    public Drone(String name, Scheduler scheduler) {
        this(name, scheduler, new DroneSpecifications(10, 30));
    }

    /**
     * Transitions from the current state to a new state.
     * {@link DroneState#triggerExitWork(Drone)} is invoked on the current state before the transition,
     * after which {@link DroneState#triggerEntryWork(Drone)} is invoked on the new state.
     *
     * @param state the new state to transition to
     */
    public void transitionState(Class<? extends DroneState> state) throws InterruptedException {
        this.state.triggerExitWork(this);
        this.state = STATES.retrieve(state);
        this.state.triggerEntryWork(this);
    }

    //Start the Drone, wait for notifications.
    @Override
    public void run() {
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
        }
        System.out.println(Thread.currentThread().getName() + " is shutting down.");
    }

    /**
     * Ticks the physics of this Drone for the period of time since the last time it was ticked.
     */
    private void tickPhysics() {
        // calculate & update timing
        double now = Instant.now().toEpochMilli();
        double tickTime = now - lastTickTimeMillis;
        lastTickTimeMillis = now;

        // Simulate physics for the period of (lastTickTimeMillis) to (now).
        kinematics.tick(tickTime);
    }

    protected boolean InZoneSchedulerResponse(){
        return (this.scheduler.confirmDroneInZone(this));
    }

    protected void extinguishFlames() throws InterruptedException {
        System.out.println(name + " is extinguishing flames!");
        nozzle.extinguish();
    }

    // Note for Zane: If you are implementing movement, you should chance this to the actual x and y of the Drone.
    public Vector2d getPosition() {
        return kinematics.getPosition();
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
