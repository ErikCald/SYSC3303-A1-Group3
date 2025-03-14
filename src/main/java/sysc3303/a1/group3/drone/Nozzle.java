package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Severity;

public class Nozzle {
    boolean open;
    boolean stuck;
    final int droneNozzleOpenTime = 1000;

    WaterTank tank;

    public Nozzle(WaterTank tank) {
        this.tank = tank;
    }

    public void extinguish(Severity severity, String name) throws InterruptedException {
        activate();
        if (severity.equals(Severity.High)){
            tank.releaseWater(15, name);
        }else {
            tank.releaseWater(10, name);
        }
        deactivate();
    }

    public void activate() throws InterruptedException {
        open = true;
        Thread.sleep(droneNozzleOpenTime);
        stuck = false;
    }

    public void deactivate() throws InterruptedException {
        Thread.sleep(droneNozzleOpenTime);
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

    //This has no consequences right now
    public void nozzleStuck(){
        stuck = true;
    }
    public void nozzleUnStuck(){
        stuck = false;
    }
}
