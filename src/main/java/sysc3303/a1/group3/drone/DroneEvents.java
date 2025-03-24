package sysc3303.a1.group3.drone;

import java.sql.Time;


class DroneEvent {
    private Time time;
    private String droneId;
    private String eventType;

    public DroneEvent(Time time, String droneId, String eventType) {
        this.time = time;
        this.droneId = droneId;
        this.eventType = eventType;
    }

    public Time getTime() {
        return time;
    }

    public String getDroneId() {
        return droneId;
    }

    public String getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "DroneEvent{" +
            "time=" + time.toString() +
            ", droneId=" + droneId +
            ", eventType='" + eventType + '\'' +
            '}';
    }
}
