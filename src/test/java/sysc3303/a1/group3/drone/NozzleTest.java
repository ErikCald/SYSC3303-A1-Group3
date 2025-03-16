package sysc3303.a1.group3.drone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.Severity;

import static org.junit.jupiter.api.Assertions.*;

class NozzleTest {

    private WaterTank waterTank;
    private Nozzle nozzle;
    private Event event;

    @BeforeEach
    void setUp() {
        // Create a new water tank with 15 liters of water (default)
        waterTank = new WaterTank();

        // Create the Nozzle object with the water tank
        nozzle = new Nozzle(waterTank);

        // Create a mock event
        event = new Event(java.sql.Time.valueOf("00:00:10"), 1, Event.EventType.FIRE_DETECTED, Severity.High);
    }

    @Test
    void testSetupExtinguishing() {
        // Simulate the nozzle setup for extinguishing
        nozzle.setupExtinguishing(Severity.High, "drone1");

        // Assert that the nozzle is open after setup
        assertTrue(nozzle.isOpen(), "Nozzle should be open after setup.");

        assertEquals(15, waterTank.getWaterLevel(), "Water level should match after setup.");
    }

    @Test
    void testOpenAndCloseNozzle() throws InterruptedException {
        // Set up extinguishing first
        nozzle.setupExtinguishing(Severity.Moderate, "drone1");

        // Simulate extinguishing the fire
        nozzle.finishExtinguishing("drone1", event);

        // Assert that the nozzle is closed after finishing extinguishing
        assertFalse(nozzle.isOpen(), "Nozzle should be closed after finishing extinguishing.");
    }

}
