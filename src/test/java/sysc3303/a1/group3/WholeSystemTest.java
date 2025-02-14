package sysc3303.a1.group3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sysc3303.a1.group3.drone.Drone;

import java.io.IOException;
import java.io.InputStream;

public class WholeSystemTest {

    private Scheduler scheduler;
    private FireIncidentSubsystem fiSubsystem;
    private Drone drone;

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

    // Test the whole system, similar to main
    @Test
    void SystemTest() {

        //Make threads from the aforementioned objects
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread DroneThread = new Thread((drone), "Drone");

        // Start the simulation
        FIsubsystemThread.start();
        DroneThread.start();

        //wait for threads to be finished.
        try {
            FIsubsystemThread.join();
            DroneThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //If no errors are shown, then the system is working.
        //Unfortunately, at this point, we can't test for much more as we are just printing things out.
        //In future iterations, we can assert(if fire is put out) or anything similar.
    }

}
