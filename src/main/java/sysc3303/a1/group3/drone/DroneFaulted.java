package sysc3303.a1.group3.drone;

public class DroneFaulted implements DroneState {

    @Override
    public void runState(Drone drone) {
        throw new UnsupportedOperationException("To be implemented in iteration 4");
    }

    @Override
    public DroneState getNextState(Drone drone) {
        throw new UnsupportedOperationException("To be implemented in iteration 4");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
