package sysc3303.a1.group3.drone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sysc3303.a1.group3.Parser;

import static org.junit.jupiter.api.Assertions.*;

public class DroneStatesTest {


    Drone drone1;
    DroneState droneIdle;

    String schedulerAddress = "localhost"; // Scheduler's IP
    int schedulerPort = 6002; // Scheduler's port

    Parser parser;

    @BeforeEach
    void beforeEach() {
        parser = new Parser();
        droneIdle = new DroneIdle();

        drone1 = new Drone("drone1", schedulerAddress, schedulerPort, parser.getZones());
    }

    @Test
    void testRetrieve() {
        // Ensure an instance can be registered and retrieved
        assertSame(droneIdle, drone1.getState());
    }

    @Test
    void testStateSequence() {
        // Ensure an instance is not retrieved if it hasn't been registered
        drone1.getState().runState(drone1);
    }
}
