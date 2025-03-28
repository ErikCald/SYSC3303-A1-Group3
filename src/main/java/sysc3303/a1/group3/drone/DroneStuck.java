package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneStuck implements DroneState {
    private boolean isFaultHandled = false;

    @Override
    public void runState(Drone drone) {
        if (!isFaultHandled) {
            System.out.println("Drone " + drone.getName() + " is stuck. Communicating shutdown to scheduler then shutting down.");
            drone.handleFault(this);
            isFaultHandled = true;
        }
    }

    @Override
    public DroneState getNextState(Drone drone) {
        return this;
    }

    @Override
    public void onNewEvent(Drone drone, Event event) {
        System.err.println("[Error3303]: Drone " + drone.getName() + " is stuck and cannot accept new events.");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}