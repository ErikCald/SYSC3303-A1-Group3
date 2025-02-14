package sysc3303.a1.group3.drone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DroneStatesTest {

    DroneStates droneStates;
    DroneState droneIdle;

    @BeforeEach
    void beforeEach() {
        droneStates = new DroneStates();
        droneIdle = new DroneIdle();
    }

    @Test
    void testRetrieve() {
        droneStates.register(droneIdle);
        // Ensure an instance can be registered and retrieved
        assertSame(droneIdle, droneStates.retrieve(DroneIdle.class));
    }

    @Test
    void testNotRegistered() {
        // Ensure an instance is not retrieved if it hasn't been registered
        assertThrows(IllegalArgumentException.class, () -> droneStates.retrieve(DroneIdle.class));
    }

    @Test
    void testAlreadyRegistered() {
        droneStates.register(droneIdle);
        // Ensure no instance of the same type can be registered
        assertThrows(IllegalArgumentException.class, () -> droneStates.register(droneIdle));
        assertThrows(IllegalArgumentException.class, () -> droneStates.register(new DroneIdle()));
    }

    @Test
    void testWithDefaults() {
        // Ensure that static factory provides defaults
        droneStates = DroneStates.withDefaults();
        assertInstanceOf(DroneIdle.class, droneStates.retrieve(DroneIdle.class));
    }
}
