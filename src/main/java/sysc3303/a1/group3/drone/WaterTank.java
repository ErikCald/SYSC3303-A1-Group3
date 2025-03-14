package sysc3303.a1.group3.drone;

public class WaterTank {
    private float waterLevel;
    private final float rateOfFlowS = 5;
    private final float rateOfFlowMS = rateOfFlowS/1000;
    public WaterTank() {
        waterLevel = 15;
    }

    public void releaseWater(int amount) throws InterruptedException {
        //This is where the water tank should be adjusted in the future.
        System.out.println("the drone releases some water.");
        float timeToWait = amount/rateOfFlowMS;
        for (int i = 0; (float) i < timeToWait; i++){
            Thread.sleep(1000);
            reduceWaterLevel(rateOfFlowS);
        }

    }

    public float getWaterLevel() {return waterLevel;}

    public void reduceWaterLevel(float amount) {
        this.waterLevel -= amount;
        System.out.println(this.toString() + "  Level is now: " + getWaterLevel());
    }

    public void fillWaterLevel(){
        this.waterLevel = 15;
    }
}
