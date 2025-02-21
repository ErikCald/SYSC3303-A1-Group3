package sysc3303.a1.group3.drone;

public class DroneEnRoute implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) throws InterruptedException {
        //Simulate movement
        Thread.sleep(500);
        drone.transitionState(DroneReturning.class);
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // todo ?
    }

    @Override
    public void onZoneArrival(Drone drone) throws InterruptedException {
        drone.transitionState(DroneInZone.class);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
