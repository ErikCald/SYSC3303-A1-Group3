package sysc3303.a1.group3.drone;

public class DroneIdle implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) {
        // todo tell scheduler that drone arrived at the base? does the scheduler or the drone initiate foam reloading?
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // todo ?
    }

    @Override
    public void onZoneInstruction(Drone drone) {
        drone.transitionState(DroneEnRoute.class);
    }
}
