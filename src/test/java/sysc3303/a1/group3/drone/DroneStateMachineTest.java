package sysc3303.a1.group3.drone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sysc3303.a1.group3.FireIncidentSubsystem;
import sysc3303.a1.group3.Parser;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.Zone;

public class DroneStateMachineTest {

    /**
     * A testable version of the Drone class that allows for the onStateChange callback to be set.
     */
    public static class TestableDrone extends Drone {
        public TestableDrone(String name, String schedulerAddress, int schedulerPort, List<Zone> zones) {
            super(name, schedulerAddress, schedulerPort, zones);
            stateHistory.add(DroneIdle.class);
        }

        ArrayList<Class<? extends DroneState>> stateHistory = new ArrayList<>();

        @Override
        public void transitionState(DroneState state) {
            stateHistory.add(state.getClass());
            super.transitionState(state);
        }

        public ArrayList<Class<? extends DroneState>> getStateHistory() {
            return stateHistory;
        }
    }

    public Scheduler scheduler;
    private TestableDrone drone;
    private FireIncidentSubsystem fiSubsystem;

    @BeforeEach
    void beforeEach() throws IOException {
        // Create a fresh Scheduler and Drone for each test

        InputStream incidentFile = DroneStateMachineTest.class.getResourceAsStream("/stateMachineTestIncidentFile.csv");
        InputStream zoneFile = DroneStateMachineTest.class.getResourceAsStream("/zoneLocationsForTesting.csv");
        String schedulerAddress = "localhost"; // Scheduler's IP
        int schedulerPort = 6011; // Scheduler's port

        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(incidentFile);
            parser.parseZoneFile(zoneFile);
        } catch (IOException e) {
            fail("Failed to parse incident file or zone location file, aborting.");
        }

        drone = new TestableDrone("drone1", schedulerAddress, schedulerPort, parser.getZones());
        scheduler = new Scheduler(parser.getZones(), schedulerPort);
        fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);
    }

//    @Test
//    public void testSingleDroneStateMachine() {
//        // Create threads for the subsystems
//        Thread fiSubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
//        Thread droneThread = new Thread(drone, "Drone");
//
//        ArrayList<Class<? extends DroneState>> expectedStatesInOrder = new ArrayList<>();
//        expectedStatesInOrder.add(DroneIdle.class);
//        expectedStatesInOrder.add(DroneEnRoute.class);
//        expectedStatesInOrder.add(DroneInZone.class);
//        expectedStatesInOrder.add(DroneDroppingFoam.class);
//        expectedStatesInOrder.add(DroneReturning.class);
//        expectedStatesInOrder.add(DroneIdle.class);
//
//        // Schedule the drone thread, which will request events when running
//        DroneState currentState = drone.getState();
//        assertEquals(expectedStatesInOrder.getFirst(), currentState.getClass());
//        droneThread.start();
//
//        // Start the simulation which will send an event to the drone and change its state
//        fiSubsystemThread.start();
//
//        // Wait for the threads to finish
//        try {
//            fiSubsystemThread.join();
//            droneThread.join();
//        } catch (InterruptedException e) {
//            fail("Thread interrupted while waiting for completion. Exception: " + e);
//        }
//
//        // Ensure the drone's state history matches the expected states
//        for (int i = 1; i < expectedStatesInOrder.size(); i++) {
//            assertEquals(expectedStatesInOrder.get(i), drone.getStateHistory().get(i), "Drone state history does not match expected state at index " + i);
//        }
//    }
}
