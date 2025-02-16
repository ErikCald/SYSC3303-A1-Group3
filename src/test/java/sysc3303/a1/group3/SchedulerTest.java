package sysc3303.a1.group3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sysc3303.a1.group3.drone.Drone;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/*
NOTE: confirmWithSubsystem is not tested here, as well as other synchronized methods as they
depend on other classes to operate. See WholeSystemTest for this
 */

class SchedulerTest {

    private Scheduler scheduler;
    private Drone drone;
    private FireIncidentSubsystem fiSubsystem;

    InputStream fileStream;

    @BeforeEach
    void beforeEach() throws IOException {
        fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        scheduler = new Scheduler();
        fiSubsystem = new FireIncidentSubsystem(scheduler, fileStream);
        drone = new Drone(scheduler);

        scheduler.addDrone(drone);
        scheduler.setSubsystem(fiSubsystem);
    }

    // Add an event, and test if it is there afterward, and equal to the one prior
    @Test
    void testAddEvent() {
        Event event = new Event(new java.sql.Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);
        scheduler.addEvent(event);

        Event removedEvent = scheduler.removeEvent();
        assertEquals(event, removedEvent);
    }

    //Similar to before, but testing if the drone shuts off afterward.
    @Test
    void testShutOff() throws InterruptedException {
        Thread droneThread = new Thread(drone, "Drone");
        droneThread.start();

        Event event = new Event(new java.sql.Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);
        scheduler.addEvent(event);

        scheduler.shutOff();

        // Verify that the shutoff flag is set and that the drone is off and has no event.
        assertTrue(scheduler.getShutOff());
    }

}
