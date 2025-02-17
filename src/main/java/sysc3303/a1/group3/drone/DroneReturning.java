package sysc3303.a1.group3.drone;

public class DroneReturning implements BaseDroneState {

    // similar to DroneEnRoute?

    @Override
    public void triggerEntryWork(Drone drone) throws InterruptedException {
        //Simulate movement
        Thread.sleep(500);
        drone.transitionState(DroneIdle.class);
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // todo
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
