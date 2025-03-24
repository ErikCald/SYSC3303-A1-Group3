package sysc3303.a1.group3;

import java.sql.Time;
import java.util.Calendar;

/**
 * Represents a fire incident event.
 */
public class Event {
    public enum EventType {
        FIRE_DETECTED,
        DRONE_REQUESTED;


        public static EventType fromString(String string) {
            return EventType.valueOf(string);
        }
    }

    private final Time time;
    private final int zoneId;
    private final EventType eventType;
    private final Severity severity;

    public Event(Time time, int zoneId, EventType eventType, Severity severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
    }

    // Returns the time as seconds since midnight.
    public int getTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        return ((hours * 3600) + (minutes * 60) + seconds);
    }

    public int getZoneId() {
        return zoneId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return String.format("Event: %s, Zone: %d, Type: %s, Severity: %s", time, zoneId, eventType, severity);
    }

    @Override
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
