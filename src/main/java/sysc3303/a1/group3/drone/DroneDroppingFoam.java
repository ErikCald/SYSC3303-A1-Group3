package sysc3303.a1.group3.drone;

public class DroneDroppingFoam implements DroneState {

    @Override
    public void runState(Drone drone) {
        drone.extinguishFlames();
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.isTankEmpty()) {
            return new DroneReturning();
        }

        return this;
    }


    @Override
    public String toString() {
        return getStateName();
    }
}
