package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

/**
 * A type of drone state, allowing for customizable event handling related to drones.
 */
public interface DroneState {
    /**
     * Run the state's logic.
     * 
     * @param drone the drone in this state
     */
    void runState(Drone drone);

    /**
     * Check if the drone should transition to a new state.
     * 
     * @param drone the drone in this state
     * @return the next state to transition to, or the current state if no transition is needed
     */
    DroneState getNextState(Drone drone);

    default void onNewEvent(Drone drone, Event event) {
        throw new UnsupportedOperationException("This state (" + getStateName() + ") does not handle new events");
    }

    /**
     * Action to run when the drone occurs a fault.
     * 
     * @param drone the drone that faulted
     */
    default void onFault(Drone drone) {
        throw new UnsupportedOperationException("To be implemented in iteration 4");
    }

    /**
     * Get the name of the state.
     * @return
     */
    default String getStateName() {
        return this.getClass().getSimpleName();
    }
}
