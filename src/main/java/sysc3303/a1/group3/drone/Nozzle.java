package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Severity;
import sysc3303.a1.group3.physics.Vector2d;
import sysc3303.a1.group3.Main;
public class Nozzle {
    private final int DRONE_NOZZLE_OPEN_TIME = 1000;

    private boolean open;
    private boolean stuck;

    private final WaterTank tank;
    private double targetWaterLevel;
    private double litersDropped;

    public Nozzle(WaterTank tank) {
        this.tank = tank;
    }

    public void setupExtinguishing(Severity severity, String droneName) {
        targetWaterLevel = Math.max(0, tank.getWaterLevel() - severity.getRequiredLetersOfFoam());
        litersDropped = tank.getWaterLevel() - targetWaterLevel;
        activate(droneName);
    }

    public void extinguish(String name) {
        tank.reduceWaterLevel(name);
    }
    
    public boolean isFinishedExtinguishing() {
        return tank.getWaterLevel() <= targetWaterLevel;
    }

    public void finishExtinguishing(String droneName, Event event) {
        deactivate(droneName, event);
    }

    private void activate(String droneName) {
        open = true;
        System.out.printf("Drone %s has started extinguishing flames! Dropping %.0f liters of foam.\n", droneName, litersDropped);
        try {
            Thread.sleep(DRONE_NOZZLE_OPEN_TIME);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted trying to sleep to simulate nozzle opening");
        }
        stuck = false;
    }

    private void deactivate(String droneName, Event event) {
        try {
            Thread.sleep(DRONE_NOZZLE_OPEN_TIME);
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted trying to sleep to simulate nozzle closing");
        }

        if (tank.getWaterLevel() <= 0.0) {
            System.out.println("[" + Main.getElapsedTime() + "] " + "Drone " + droneName + " has run out of foam in the middle of extinguishing the flames of event: " + event + " and is returning.");
        } else {
            System.out.println("[" + Main.getElapsedTime() + "] " +  "Drone " + droneName + " has finished extinguishing the flames of event: " + event + " and is returning.");
        }
        
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public void nozzleStuck(){
        stuck = true;
    }
    public void nozzleUnStuck(){
        stuck = false;
    }

    public boolean isStuck() {
        return stuck;
    }

    public void repairNozzle(Vector2d dronePosition) {
        double distance = dronePosition.magnitude();
        // Wait time: base 500 ms plus 10 ms per unit distance.
        long waitTime = 500 + (long)(distance * 10);
        System.out.printf("Repairing nozzle at drone position %s. Waiting for %d ms for repair...\n", dronePosition, waitTime);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Nozzle repair interrupted.");
        }
        System.out.printf("Finish nozzle repair at position %s.\n", dronePosition + ". Drone returning to previous task.");
        nozzleUnStuck();
    }

}
