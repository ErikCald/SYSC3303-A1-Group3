package sysc3303.a1.group3.drone;

public class Sensors {

    //will probably have more stuff when we get to drone faults.
    //right now, the only thing I can think of is this pretty pointless function lol.

    public boolean hasArrived(int currentX, int currentY, int targetX, int targetY) {
        return currentX >= targetX && currentY >= targetY;
    }


}
