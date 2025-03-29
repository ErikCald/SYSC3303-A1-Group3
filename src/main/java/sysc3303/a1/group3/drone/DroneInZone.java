package sysc3303.a1.group3.drone;

public class DroneInZone implements DroneState {

    @Override
    public void runState(Drone drone) {
        // do nothing
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.getNozzle().isStuck()){
            drone.handleFault(this, new DroneInZone());
        }

        if (drone.isInZoneSchedulerResponse()){
            System.out.println("Drone " + drone.getName() + " is extinguishing flames!");
            return new DroneDroppingFoam();
        } else {
            return new DroneFaulted();
        }
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
