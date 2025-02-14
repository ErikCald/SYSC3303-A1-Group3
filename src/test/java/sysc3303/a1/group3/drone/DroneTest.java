package sysc3303.a1.group3.drone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.FireIncidentSubsystem;
import sysc3303.a1.group3.Main;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.Severity;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

public class DroneTest {

    Scheduler scheduler;
    Drone drone;
    FireIncidentSubsystem fiSubsystem;
    InputStream fileStream;

    @BeforeEach
    void beforeEach() throws IOException {
        // Create a fresh Scheduler and Drone for each test
        fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        scheduler = new Scheduler();
        drone = new Drone(scheduler);
        fiSubsystem = new FireIncidentSubsystem(scheduler, fileStream);
        scheduler.addDrone(drone);
        scheduler.setSubsystem(fiSubsystem);
    }

    @Test
    void testIdlingDrone() {
        // A newly created drone should have no event/task
        assertNull(drone.currentEvent);
    }

    @Test
    void testDroneThreadBlocked() throws TimeoutException {
        // Schedule the drone thread
        Thread droneThread = new Thread(drone, "Drone");
        droneThread.start();

        // Since the Scheduler hasn't been populated with anything, the drone thread should enter a waiting state
        // shortly after running. There is no assertion here, but if the droneThread doesn't enter a WAITING state
        // within 10ms, then the test fails (exception).
        busyWaitUntil(() -> droneThread.getState() == Thread.State.WAITING, 10);
    }

    @Test
    void testRequestEvent() {
        // Dummy event
        Event event = new Event(new Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);

        // Populate the Scheduler with a single event
        scheduler.addEvent(event);
        // Request the single event from the scheduler
        drone.requestEvent();

        // Ensure the event is the same
        assertEquals(event, drone.currentEvent);
    }

    @Test
    void testRequestEventThreaded() throws TimeoutException {
        // Schedule the drone thread, which will request events when running
        Thread droneThread = new Thread(drone, "Drone");
        droneThread.start();

        // Populate the Scheduler with a single event
        Event event = new Event(new Time(0), 0, Event.EventType.DRONE_REQUESTED, Severity.High);
        scheduler.addEvent(event);

        // Wait until the drone's event field is filled
        busyWaitUntil(() -> drone.currentEvent != null, 10);

        // Ensure the event that the drone received is the same as what was put in the scheduler
        assertEquals(event, drone.currentEvent);
    }

    /**
     * This method runs until the provided condition returns true.
     *
     * @param condition the condition to await
     * @param timeout the maximum amount of time in milliseconds to wait for
     * @throws TimeoutException if the timeout is exceeded
     */
    private void busyWaitUntil(BooleanSupplier condition, long timeout) throws TimeoutException {
        // todo: will they let us use https://github.com/awaitility/awaitility for testing?
        long start = System.currentTimeMillis();
        while (!condition.getAsBoolean()) {
            if (start - System.currentTimeMillis() >= timeout) {
                throw new TimeoutException("Wait time exceeded %sms".formatted(timeout));
            }
        }
    }
}
