package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneNozzleJam implements DroneState {
    DroneState prevState;

    public DroneNozzleJam(DroneState prevState){
        this.prevState = prevState;
    }

    @Override
    public void runState(Drone drone) {
        drone.handleFault(new DroneNozzleJam(prevState), prevState);
    }

    @Override
    public DroneState getNextState(Drone drone) {
        return prevState;
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
