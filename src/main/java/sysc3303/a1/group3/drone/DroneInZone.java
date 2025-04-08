package sysc3303.a1.group3.drone;

public class DroneInZone implements DroneState {

    @Override
    public void runState(Drone drone) {
        // do nothing
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.getNozzle().isStuck()){
//            drone.handleFault(this, new DroneInZone());
            return new DroneNozzleJam(new DroneEnRoute());
        }

        drone.setTargetZone();

        if (drone.isAtZone()){
            System.out.println("Drone " + drone.getName() + " is extinguishing flames!");
            return new DroneDroppingFoam();
        } else {
            return new DroneEnRoute();
        }
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
