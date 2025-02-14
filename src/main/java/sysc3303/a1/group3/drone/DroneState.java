package sysc3303.a1.group3.drone;

/**
 * A type of drone state, allowing for customizable event handling related to drones.
 */
public interface DroneState {

    /**
     * Trigger for when this state becomes the current state.
     *
     * @param drone the Drone entering this state
     */
    void triggerEntryWork(Drone drone);

    /**
     * Trigger for when this state is no longer the current state.
     *
     * @param drone the Drone exiting this state
     */
    void triggerExitWork(Drone drone);

    /**
     * Event handler for the Scheduler instructing a drone that it should go to a zone.
     *
     * @param drone the drone being instructed
     */
    void onZoneInstruction(Drone drone);
    // todo more parameters needed?

    /**
     * Event handler for when a drone arrives at the destination zone.
     *
     * @param drone the drone that arrived
     */
    void onZoneArrival(Drone drone);

    /**
     * Event handler for the Scheduler instructing a drone to begin dropping foam.
     *
     * @param drone the drone being instructed
     */
    void onDropInstruction(Drone drone);

    /**
     * Event handler for a drone completing a foam drop.
     *
     * @param drone the drone that completed a foam drop
     */
    void onDropComplete(Drone drone);

    /**
     * Event handler for a drone arriving at the base
     *
     * @param drone the drone that arrived at the base
     */
    void onBaseArrival(Drone drone);

    /**
     * Event handler for the system beginning shutdown
     *
     * @param drone the drone being instructed to comply with the shutdown
     */
    void onShutdown(Drone drone);

    /**
     * Event handler for a drone experiencing a fault
     *
     * @param drone experiencing a fault
     */
    void onFault(Drone drone);

    /**
     * Throws an {@link IllegalArgumentException} with a message indicating
     * that an event handler method was invoked in an unexpected state.
     *
     * @param drone the drone that the event handler was invoked for
     * @throws IllegalStateException always thrown
     */
    default void throwIllegalState(Drone drone) throws IllegalStateException {
        throw new IllegalStateException(drone + " is in inappropriate state " + this.getClass().getSimpleName());
    }
}
