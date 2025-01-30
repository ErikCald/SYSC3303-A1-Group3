package sysc3303.a1.group3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/*
NOTE: As mentioned in other test files, if you find that synchronized method tests are missing, then they're
tested in WholeSystemTest as these methods can only be meaningfully tested there.
 */

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    Scheduler scheduler;
    Drone drone;
    FireIncidentSubsystem fiSubsystem;

    @BeforeEach
    void beforeEach() {
        scheduler = new Scheduler();
        drone = new Drone(scheduler);
        fiSubsystem = new FireIncidentSubsystem(scheduler);
        scheduler.addDrone(drone);
        scheduler.setSubsystem(fiSubsystem);
    }

    // After parsing, there should be some Events. If an error occurs, or is empty, then fail.
    @Test
    void testParseEvents() {
        fiSubsystem.parseEvents();
        assertFalse(fiSubsystem.getEvents().isEmpty(), "The events list should not be empty.");
    }

    // Parse events some evens and start the subsystem
    // After the subsystem has finished, this should mean all events are exhausted.
    // This test may not be relevant in later iterations.
    @Test
    void testRunSubsystem() throws InterruptedException {
        // Add events to the subsystem (simulate file parsing)
        fiSubsystem.parseEvents();

        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        FIsubsystemThread.start();

        //wait for the subsystem to finish.
        FIsubsystemThread.join();
        assertTrue(scheduler.getShutOff());
    }

}
