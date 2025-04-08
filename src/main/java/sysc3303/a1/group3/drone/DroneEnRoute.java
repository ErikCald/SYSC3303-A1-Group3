package sysc3303.a1.group3.drone;
import sysc3303.a1.group3.Main;
import sysc3303.a1.group3.Event;

public class DroneEnRoute implements DroneState {

    @Override
    public void runState(Drone drone) {
        drone.moveToZone();
    }

    @Override
    public DroneState getNextState(Drone drone) {
        if(drone.isStuck()) {
            return new DroneStuck(this);
        }

        if (drone.getNozzle().isStuck()){
//            drone.handleFault(this, new DroneEnRoute());
            return new DroneNozzleJam(new DroneEnRoute());
        }

        if (drone.isAtZone()) {
            if(drone.getCurrentEvent().isEmpty()) {
                System.err.println("[Error3303]: Drone " + drone.getName() + " has arrived at Zone " + drone.getCurrentEvent().get().getZoneId() + " but has no event. Returning.");
                return new DroneReturning();
            }

            System.out.println("[" + Main.getElapsedTime() + "] " +  "Drone " + drone.getName() + " has arrived at Zone " + drone.getCurrentEvent().get().getZoneId() + ".");
            return new DroneInZone();
        }

        return this;
    }

    @Override
    public void onNewEvent(Drone drone, Event event) {
        System.out.println("[" + Main.getElapsedTime() + "] " +  drone.getName() + " changed routes and is now en route to Zone " + event.getZoneId() + ".");
        drone.setCurrentEvent(event);
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
