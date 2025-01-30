package sysc3303.a1.group3;



public class Zone {
    private int zoneID;
    private int start_x;
    private int start_y;
    private int end_x;
    private int end_y;

    /**
     * Create a new zone.
     *
     * @param zoneID The time the event was requested.
     * @param start_x The zone the event was requested from.
     * @param start_y The type of event.
     * @param end_x The severity of the event.
     * @param end_y The severity of the event.
     */
    public Zone(int zoneID,int start_x, int start_y, int end_x, int end_y){
        this.zoneID = zoneID;
        this.start_x = start_x;
        this.start_y = start_y;
        this.end_x = end_x;
        this.end_y = end_y;
    }

    public int getZoneID() {
        return zoneID;
    }

    public int[] getlocation (int zoneID){
        return new int[]{start_x, start_y, end_x, end_y};
    }

}
