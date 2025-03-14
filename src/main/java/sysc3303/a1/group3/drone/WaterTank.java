package sysc3303.a1.group3.drone;

public class WaterTank {
    private float waterLevel;
    private final float rateOfFlowS = 5;
    //private final float rateOfFlowMS = rateOfFlowS/1000;
    public WaterTank() {
        waterLevel = 15;
    }

    public void releaseWater(int amount, String name) throws InterruptedException {
        //This is where the water tank should be adjusted in the future.
        System.out.println("the drone releases some water.");
        float timeToWait = amount/rateOfFlowS;
        for (int i = 0; (float) i < timeToWait; i++){
            Thread.sleep(1000);
            reduceWaterLevel(rateOfFlowS, name);
            if (waterLevelEmpty()){
                break;
            }
        }

    }

    public float getWaterLevel() {return waterLevel;}

    public void reduceWaterLevel(float amount, String name) {
        this.waterLevel -= amount;
        System.out.println(name + "  Level is now: " + getWaterLevel());
    }

    public void fillWaterLevel(){
        this.waterLevel = 15;
    }

    public boolean waterLevelEmpty(){
        return waterLevel <= 0;
    }
}
