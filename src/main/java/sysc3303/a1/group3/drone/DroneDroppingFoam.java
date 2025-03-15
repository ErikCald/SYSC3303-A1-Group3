package sysc3303.a1.group3.drone;

public class DroneDroppingFoam implements DroneState {
    private boolean isDroppingFoam = false;

    @Override
    public void runState(Drone drone) {
        if(!isDroppingFoam) {
            if(drone.getCurrentEvent().isEmpty()) {
                throw new RuntimeException("[Error3303]: Drone " + drone.getName() + " has no event to extinguish. Returning.");
            }

            isDroppingFoam = true;
            drone.getNozzle().setupExtinguishing(drone.getCurrentEvent().get().getSeverity(), drone.getName());
        }

        drone.getNozzle().extinguish(drone.getName());
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.getNozzle().isFinishedExtinguishing()) {
            isDroppingFoam = false;
            drone.getNozzle().finishExtinguishing(drone.getName());
            return new DroneReturning();
        }

        return this;
    }


    @Override
    public String toString() {
        return getStateName();
    }
}
