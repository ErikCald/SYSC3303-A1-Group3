package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneReturning implements DroneState {


    @Override
    public void runState(Drone drone) {
        drone.moveToHome();
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.isAtHome()) {
            System.out.println(drone.getName() + " is back!");
            return new DroneIdle();
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
