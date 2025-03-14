package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneEnRoute implements DroneState {

    @Override
    public void runState(Drone drone) {
        drone.moveToZone();
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.isAtZone()) {
            return new DroneInZone();
        }

        return this;
    }

    @Override
    public void onNewEvent(Drone drone, Event event) {
        System.out.println(drone.getName() + " changed routes and is now en route to Zone " + event.getZoneId());
        drone.setCurrentEvent(event);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
