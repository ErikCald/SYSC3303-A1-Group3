package sysc3303.a1.group3.drone;

public class DroneEnRoute implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) {
        // todo simulate moving to fly height if the drone isn't already? start travel simulation?
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // todo ?
    }

    @Override
    public void onZoneArrival(Drone drone) {
        drone.transitionState(DroneInZone.class);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
