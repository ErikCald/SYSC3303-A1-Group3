package sysc3303.a1.group3.drone;

public class DroneEnRoute implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) throws InterruptedException {
        //Simulate movement
        System.out.println(drone.getName() + " on the way!");
        Thread.sleep(1000);
        drone.transitionState(DroneInZone.class);
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
