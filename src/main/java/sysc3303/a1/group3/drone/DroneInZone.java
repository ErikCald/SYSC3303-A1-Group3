package sysc3303.a1.group3.drone;

public class DroneInZone implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) {
        // todo inform scheduler that drone has arrived
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // do nothing
    }

    @Override
    public void onDropInstruction(Drone drone) {
        drone.transitionState(DroneDroppingFoam.class);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
