package sysc3303.a1.group3.drone;

public class Nozzle {
    boolean open;
    boolean stuck;

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
