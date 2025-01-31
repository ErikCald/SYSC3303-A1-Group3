package sysc3303.a1.group3;



/**
 * Represents a zone with a unique ID and coordinate boundaries.
 *
 * @param zoneID  The unique identifier for the zone.
 * @param start_x The x-coordinate of the starting point.
 * @param start_y The y-coordinate of the starting point.
 * @param end_x   The x-coordinate of the ending point.
 * @param end_y   The y-coordinate of the ending point.
 */
public record Zone(int zoneID, int start_x, int start_y, int end_x, int end_y){}




