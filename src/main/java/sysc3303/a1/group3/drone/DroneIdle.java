package sysc3303.a1.group3.drone;

public class DroneIdle implements BaseDroneState {

    @Override
    public void triggerEntryWork(Drone drone) {
        // todo tell scheduler that drone arrived at the base? does the scheduler or the drone initiate foam reloading?
        //For now, testing, remove event when getting back
        drone.fillWaterTank();
        drone.setCurrentEvent(null);
    }

    @Override
    public void triggerExitWork(Drone drone) {
        // do nothing
    }

    @Override
    public void onZoneInstruction(Drone drone) throws InterruptedException {
        drone.transitionState(DroneEnRoute.class);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
