package sysc3303.a1.group3.drone;

public class DroneFaulted implements BaseDroneState {


    @Override
    public void triggerEntryWork(Drone drone) {
        throw new UnsupportedOperationException("To be implemented in iteration 4");
    }

    @Override
    public void triggerExitWork(Drone drone) {
        throw new UnsupportedOperationException("To be implemented in iteration 4");
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
