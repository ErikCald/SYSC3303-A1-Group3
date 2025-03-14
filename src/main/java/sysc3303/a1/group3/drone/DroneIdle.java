package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneIdle implements DroneState {

    @Override
    public void runState(Drone drone) {
        drone.fillWaterTank();
        drone.setCurrentEvent(null);
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.getCurrentEvent() != null) {
            return new DroneEnRoute();
        }

        return this;
    }

    @Override
    public void onNewEvent(Drone drone, Event event) {
        System.out.println(drone.getName() + " is now en route to zone " + event.getZoneId());
        drone.setCurrentEvent(event);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
