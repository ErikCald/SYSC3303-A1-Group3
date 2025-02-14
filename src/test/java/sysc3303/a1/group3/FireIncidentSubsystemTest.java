package sysc3303.a1.group3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sysc3303.a1.group3.drone.Drone;

/*
NOTE: As mentioned in other test files, if you find that synchronized method tests are missing, then they're
tested in WholeSystemTest as these methods can only be meaningfully tested there.
 */

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    Scheduler scheduler;
    Drone drone;
    FireIncidentSubsystem fiSubsystem;

    InputStream fileStream;

    @BeforeEach
    void beforeEach() throws IOException {
        fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        scheduler = new Scheduler();
        drone = new Drone(scheduler);
        fiSubsystem = new FireIncidentSubsystem(scheduler, fileStream);
        scheduler.addDrone(drone);
        scheduler.setSubsystem(fiSubsystem);
    }

    // After create obj and parsing, there should be some Events. If an error occurs, or is empty, then fail.
    @Test
    void testParseEvents() {
        assertFalse(fiSubsystem.getEvents().isEmpty());
    }

    // Parse events some evens and start the subsystem
    // After the subsystem has finished, this should mean all events are exhausted.
    // This test may not be relevant in later iterations.
    @Test
    void testRunSubsystem() throws InterruptedException {
        Thread FIsubsystemThread = new Thread(fiSubsystem);
        FIsubsystemThread.start();

        //wait for the subsystem to finish.
        FIsubsystemThread.join();
        assertTrue(scheduler.getShutOff());
    }

}
