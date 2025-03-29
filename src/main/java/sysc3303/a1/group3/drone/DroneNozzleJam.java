package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneNozzleJam implements DroneState {

    @Override
    public void runState(Drone drone) {
        System.out.println("Drone " + drone.getName() + " has a nozzle jam. Returning to base.");
        drone.handleFault(this, new DroneNozzleJam());
    }

    @Override
    public DroneState getNextState(Drone drone) {
        return new DroneReturning();
    }

    @Override
    public void onNewEvent(Drone drone, Event event) {
        System.err.println("[Error3303]: Drone " + drone.getName() + " has a nozzle jam and cannot accept new events.");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
