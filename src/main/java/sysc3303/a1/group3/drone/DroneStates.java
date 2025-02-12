package sysc3303.a1.group3.drone;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of {@link DroneState} instances with the goal of ensuring only one instance of each type is used.
 */
public final class DroneStates {

    private final Map<Class<? extends DroneState>, DroneState> states = new HashMap<>();

    /**
     * Creates a DroneStates with no registered states.
     */
    public DroneStates() {

    }

    /**
     * Registers a state instance.
     *
     * @param state the state instance to register
     * @throws IllegalArgumentException if there is already an instance of the same class registered
     */
    public void register(DroneState state) {
        if (states.containsKey(state.getClass())) {
            throw new IllegalArgumentException("Can't register state " + state + " because an instance of " + state.getClass() + " is already registered");
        }
        states.put(state.getClass(), state);
    }

    /**
     * Retrieves the instance of the desired state.
     *
     * @param stateClass the state class desired
     * @return the instance of the state
     * @param <T> the specific state type desired
     * @throws IllegalArgumentException if there is not an instance of the desired state registered
     */
    public <T extends DroneState> T retrieve(Class<T> stateClass) {
        if (!states.containsKey(stateClass)) {
            throw new IllegalArgumentException(stateClass + " is not registered");
        }
        return stateClass.cast(states.get(stateClass));
    }

    /**
     * @return a new DroneStates instance with the default states registered
     */
    public static DroneStates withDefaults() {
        DroneStates states = new DroneStates();
        states.register(new DroneIdle());
        states.register(new DroneEnRoute());
        states.register(new DroneInZone());
        states.register(new DroneDroppingFoam());
        states.register(new DroneReturning());
        states.register(new DroneFaulted());
        return states;
    }
}
