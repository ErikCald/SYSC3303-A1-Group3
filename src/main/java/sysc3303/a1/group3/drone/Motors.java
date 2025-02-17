package sysc3303.a1.group3.drone;

public class Motors {
    private int x, y;

    public void updatePosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int[] getPosition() {
        return new int[] {x, y};  // Returning position as an array [x, y]
    }
}
