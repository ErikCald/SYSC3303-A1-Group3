package sysc3303.a1.group3;

import java.sql.Time;
import java.util.Calendar;
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
    public int getTime() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);

        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        return ((hours * 3600) + (minutes * 60) + seconds);
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

    /**
     * Get a string representation of the event.
     * 
     * @return A string representation of the event.
     */
    public String toString() {
        return String.format("Event: %s, Zone: %d, Type: %s, Severity: %s", time, zoneId, eventType, severity);
    }

    /**
     * Check if two events are equal.
     * 
     * @param other The other event to compare to.
     * @return True if the events are equal, false otherwise.
     */
    public boolean equals(Object other) {
        if(other == this) {
            return true;
        }

        if(!(other instanceof Event)) {
            return false;
        }

        Event otherEvent = (Event) other;
        return time.equals(otherEvent.time) 
            && zoneId == otherEvent.zoneId 
            && eventType == otherEvent.eventType 
            && severity == otherEvent.severity;
    }
}
