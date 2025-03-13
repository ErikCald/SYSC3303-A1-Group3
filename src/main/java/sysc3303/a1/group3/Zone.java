package sysc3303.a1.group3;


import sysc3303.a1.group3.physics.Vector2d;

/**
 * Represents a zone with a unique ID and coordinate boundaries.
 *
 * @param zoneID  The unique identifier for the zone.
 * @param startX The x-coordinate of the starting point.
 * @param startY The y-coordinate of the starting point.
 * @param endX   The x-coordinate of the ending point.
 * @param endY   The y-coordinate of the ending point.
 * @param centre The centre position of the Zone.
 */
public record Zone(int zoneID, int startX, int startY, int endX, int endY, Vector2d centre) {

    /**
     * Creates a new Zone without having to manually calculate the centre of it.
     *
     * @param zoneID  The unique identifier for the zone.
     * @param startX The x-coordinate of the starting point.
     * @param startY The y-coordinate of the starting point.
     * @param endX   The x-coordinate of the ending point.
     * @param endY   The y-coordinate of the ending point.
     * @return a new Zone
     */
    public static Zone of(int zoneID, int startX, int startY, int endX, int endY) {
        int centreX = (startX + endX) / 2;
        int centreY = (startY + endY) / 2;
        return new Zone(zoneID, startX, startY, endX, endY, Vector2d.of(centreX, centreY));
    }
}




