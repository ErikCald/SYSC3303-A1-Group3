package sysc3303.a1.group3.drone;

public class DroneDroppingFoam implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) throws InterruptedException {
        drone.extinguishFlames();
        drone.transitionState(DroneReturning.class);
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // todo close nozzles/doors?
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
