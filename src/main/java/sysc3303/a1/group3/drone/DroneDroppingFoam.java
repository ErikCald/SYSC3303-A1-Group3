package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

public class DroneDroppingFoam implements DroneState {
    private boolean isDroppingFoam = false;

    @Override
    public void runState(Drone drone) {
        if(drone.getNozzle().isStuck()) {
            return;
        }

        if (!isDroppingFoam) {
            if (drone.getCurrentEvent().isEmpty()) {
                throw new RuntimeException("[Error3303]: Drone " + drone.getName() + " has no event to extinguish. Returning.");
            }

            isDroppingFoam = true;
            drone.getNozzle().setupExtinguishing(drone.getCurrentEvent().get().getSeverity(), drone.getName());
        }

        drone.getNozzle().extinguish(drone.getName());
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if (drone.getNozzle().isStuck()) {
            return new DroneNozzleJam();
        }

        if (drone.getNozzle().isFinishedExtinguishing()) {
            isDroppingFoam = false;
            Event event = drone.getCurrentEvent().orElseThrow(() -> new IllegalStateException("No event found in DroneDroppingFoam"));
            drone.getNozzle().finishExtinguishing(drone.getName(), event);

            return new DroneReturning();
        }

        return this;
    }


    @Override
    public String toString() {
        return getStateName();
    }
}
