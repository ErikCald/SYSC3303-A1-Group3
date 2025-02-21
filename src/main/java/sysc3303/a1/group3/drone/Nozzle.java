package sysc3303.a1.group3.drone;

public class Nozzle {
    boolean open;
    boolean stuck;

    WaterTank tank;

    public Nozzle(WaterTank tank) {
        this.tank = tank;
    }

    public void extinguish() throws InterruptedException {
        activate();
        Thread.sleep(1000);
        //Placeholder for where water should be released:
        tank.releaseWater(0);
        deactivate();
    }

    public void activate() {
        open = true;
        stuck = false;
    }

    public void deactivate() {
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
