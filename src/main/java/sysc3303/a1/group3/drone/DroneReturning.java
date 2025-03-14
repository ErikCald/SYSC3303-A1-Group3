package sysc3303.a1.group3.drone;

public class DroneReturning implements BaseDroneState {

    // similar to DroneEnRoute?

    @Override
    public void triggerEntryWork(Drone drone) throws InterruptedException {
        //Simulate movement
        System.out.println(drone.getName() + " returning!");
        Thread.sleep(1000);
        drone.transitionState(DroneIdle.class);
    }

    @Override
    public void triggerExitWork(Drone drone) {
        drone.setCurrentEvent(null);
        System.out.println(drone.getName() + " is back!");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
