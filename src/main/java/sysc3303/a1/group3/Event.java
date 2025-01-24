package sysc3303.a1.group3;

import java.sql.Time;

/**
 * Represents a fire incident event.
 * Stores when, where and by who it was requested.
 */
public class Event {
    public enum EventType {
        FIRED_DETECTED,
        DRONE_REQUESTED;

        /**
         * Get the event type from a string.
         * @param string The string representation of the event type.
         * @return The event type.
         */
        public static EventType fromString(String string) {
            switch (string.toUpperCase()) {
                case "FIRE_DETECTED":
                    return FIRED_DETECTED;
                case "DRONE_REQUEST":
                    return DRONE_REQUESTED;
                default:
                    throw new IllegalArgumentException("Invalid event type: " + string);
            }
        }
    }

    private final Time time;
    private final int zoneId;
    private final EventType eventType;
    private final Severity severity;

    /**
     * Create a new event.
     * 
     * @param time The time the event was requested.
     * @param zoneId The zone the event was requested from.
     * @param eventType The type of event.
     * @param severity The severity of the event.
     */
    public Event(Time time, int zoneId, EventType eventType, Severity severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
    }

    /**
     * Get the time the event was requested.
     * 
     * @return The time the event was requested.
     */
    public Time getTime() {
        return time;
    }

    /**
     * Get the zone the event was requested from.
     * 
     * @return The zone the event was requested from.
     */
    public int getZoneId() {
        return zoneId;
    }

    /**
     * Get the type of event.
     * 
     * @return The type of event.
     */
    public EventType getEventType() {
        return eventType;
    }

    /**
     * Get the severity of the event.
     * 
     * @return The severity of the event.
     */
    public Severity getSeverity() {
        return severity;
    }

    public String toString() {
        return String.format("Event: %s, Zone: %d, Type: %s, Severity: %s", time, zoneId, eventType, severity);
    }
}
