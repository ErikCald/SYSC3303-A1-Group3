package sysc3303.a1.group3.drone;

public class DroneInZone implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) throws InterruptedException {
        if (drone.InZoneSchedulerResponse()){
            drone.transitionState(DroneDroppingFoam.class);
        } else {
            // In the future, alternate states can be met depending on scheduler confirmation
            drone.transitionState(DroneFaulted.class);
        }
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // do nothing
    }

    @Override
    public void onDropInstruction(Drone drone) throws InterruptedException {
        drone.transitionState(DroneDroppingFoam.class);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
