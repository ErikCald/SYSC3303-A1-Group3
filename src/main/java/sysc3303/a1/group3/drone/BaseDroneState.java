package sysc3303.a1.group3.drone;

/**
 * A base implementation of {@link DroneState} that makes State implementations easier.
 * Note that this does not include {@link #triggerEntryWork(Drone)} and {@link #triggerExitWork(Drone)}.
 */
public interface BaseDroneState extends DroneState {

    @Override
    default void onZoneInstruction(Drone drone) throws InterruptedException {
        throwIllegalState(drone);
    }

    @Override
    default void onZoneArrival(Drone drone) throws InterruptedException {
        throwIllegalState(drone);
    }

    @Override
    default void onDropInstruction(Drone drone) throws InterruptedException {
        throwIllegalState(drone);
    }

    @Override
    default void onDropComplete(Drone drone) {
        throwIllegalState(drone);
    }

    @Override
    default void onBaseArrival(Drone drone) {
        throwIllegalState(drone);
    }

    @Override
    default void onShutdown(Drone drone) throws InterruptedException {
        // Remember that #triggerExitWork will be invoked
        drone.transitionState(DroneReturning.class);
    }

    @Override
    default void onFault(Drone drone) {
        throw new UnsupportedOperationException("To be implemented in iteration 4");
    }
}
