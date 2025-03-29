package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneIdle implements DroneState {

    @Override
    public void runState(Drone drone) {
        drone.fillWaterTank();
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.getNozzle().isStuck()){
            drone.handleFault(this, new DroneIdle());
        }

        if (drone.getCurrentEvent().isPresent()) {
            System.out.println(drone.getName() + " is now en route to Zone " + drone.getCurrentEvent().get().getZoneId() + ".");
            return new DroneEnRoute();
        }

        return this;
    }

    @Override
    public void onNewEvent(Drone drone, Event event) {
        drone.setCurrentEvent(event);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
