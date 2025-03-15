package sysc3303.a1.group3.drone;

public class WaterTank {
    private final double RATE_OF_FLOW = 1;

    private double waterLevel;

    public WaterTank() {
        waterLevel = 15;
    }

    public void releaseWater(int amount, String droneName) throws InterruptedException {
        double timeToWait = amount / RATE_OF_FLOW;
        for (int i = 0; (double) i < timeToWait; i++){
            Thread.sleep(1000);
            reduceWaterLevel(droneName);
            
            if (waterLevelEmpty()){
                break;
            }
        }
    }

    public double getWaterLevel() {
        return waterLevel;
    }

    public void reduceWaterLevel(String droneName) {
        this.waterLevel -= RATE_OF_FLOW;
    }

    public void fillWaterLevel(){
        this.waterLevel = 15;
    }

    public boolean isFull() {
        return waterLevel == 15;
    }

    public boolean waterLevelEmpty(){
        return waterLevel <= 0;
    }
}
