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
    public void setUp() {
        // Create a new water tank with 15 liters of water (default)
        waterTank = new WaterTank();

        // Create the Nozzle object with the water tank
        nozzle = new Nozzle(waterTank);

        // Create a mock event
        event = new Event(java.sql.Time.valueOf("00:00:10"), 1, Event.EventType.FIRE_DETECTED, Severity.High);
    }

    @Test
    public void testSetupExtinguishing() {
        // Simulate the nozzle setup for extinguishing
        nozzle.setupExtinguishing(Severity.High, "drone1");

        // Assert that the nozzle is open after setup
        assertTrue(nozzle.isOpen(), "Nozzle should be open after setup.");

        // Assert that the water level matches after setup
        assertEquals(15, waterTank.getWaterLevel(), "Water level should match after setup.");

        // Assert that the nozzle is correctly reporting that it is not finished extinguishing
        assertFalse(nozzle.isFinishedExtinguishing(), "The nozzle should not be finished extinguishing yet.");
    }

    @Test
    public void testOpenAndCloseNozzle() throws InterruptedException {
        // Set up extinguishing first
        nozzle.setupExtinguishing(Severity.Moderate, "drone1");

        // Simulate extinguishing the fire
        nozzle.finishExtinguishing("drone1", event);

        // Assert that the nozzle is closed after finishing extinguishing
        assertFalse(nozzle.isOpen(), "Nozzle should be closed after finishing extinguishing.");
    }

    @Test
    public void testExtinguish() {
        // Set up extinguishing first
        nozzle.setupExtinguishing(Severity.Moderate, "drone1");

        // Simulate extinguishing the fire
        int i = 0;
        while (!nozzle.isFinishedExtinguishing()) {
            nozzle.extinguish("drone1");
            i++;

            assertTrue(i < 30, "Extinguishing should finish within 30 iterations.");
        }

        // Assert that the nozzle is finished extinguishing
        nozzle.finishExtinguishing("drone1", event);

        // Assert that the tank should be empty after extinguishing a moderate fire
        assertEquals(0, waterTank.getWaterLevel(), "Water level should be 0 after extinguishing a moderate fire.");
    }

}
