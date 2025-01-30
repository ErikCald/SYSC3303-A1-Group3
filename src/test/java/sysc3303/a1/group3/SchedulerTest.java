package sysc3303.a1.group3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
NOTE: confirmWithSubsystem is not tested here, as well as other synchronized methods as they
depend on other classes to operate. See WholeSystemTest for this
 */

class SchedulerTest {

    private Scheduler scheduler;
    private Drone drone;
    private FireIncidentSubsystem fiSubsystem;

    @BeforeEach
    void beforeEach() {
        // Initialize the objects before each test
        scheduler = new Scheduler();
        fiSubsystem = new FireIncidentSubsystem(scheduler);
        drone = new Drone(scheduler);

        scheduler.addDrone(drone);
        scheduler.setSubsystem(fiSubsystem);
    }

    @Test
    void testAddEvent() {
        Event event = new Event(new java.sql.Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);
        scheduler.addEvent(event);

        Event removedEvent = scheduler.removeEvent();
        assertEquals(event, removedEvent);
    }

    // Waits are to guarantee the JVM has given the Drone time to run
    @Test
    void testRemoveEventEmptyQueue() throws InterruptedException {
        Thread droneThread = new Thread(drone, "Drone");
        droneThread.start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Since no events, the drone should wait for events
        assertEquals(Thread.State.WAITING, droneThread.getState());

        Event event = new Event(new java.sql.Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);
        scheduler.addEvent(event);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify that the drone received the event, and that it equals the one prior
        assertNotNull(drone.currentEvent);
        assertEquals(event, drone.currentEvent);
    }

    //Similar to before, but testing if the drone shuts off.
    @Test
    void testShutOff() throws InterruptedException {
        Thread droneThread = new Thread(drone, "Drone");
        droneThread.start();

        Event event = new Event(new java.sql.Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);
        scheduler.addEvent(event);

        scheduler.shutOff();

        // Verify that the shutoff flag is set and that the drone is off and has no event.
        assertTrue(scheduler.getShutOff());
        assertNull(drone.currentEvent);
    }

}
