package sysc3303.a1.group3.drone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Isolated;

import sysc3303.a1.group3.FireIncidentSubsystem;
import sysc3303.a1.group3.Parser;
import sysc3303.a1.group3.Scheduler;
import sysc3303.a1.group3.Zone;

@Isolated
public class DroneStateMachineTestWithFaults {

    /**
     * A testable version of the Drone class that allows for the onStateChange
     * callback to be set.
     */
    public static class TestableDrone extends Drone {
        public TestableDrone(String name, String schedulerAddress, int schedulerPort, Map<Integer, Zone> zones,
                String faultFile) {
            super(name, schedulerAddress, schedulerPort, zones, faultFile);
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
    private String schedulerAddress = "localhost"; // Scheduler's IP
    private int test1SchedulerPort = 6021; // Scheduler's port
    private int test2SchedulerPort = 6015; // Scheduler's port
    private int test3SchedulerPort = 6016; // Scheduler's port

    @Test
    @Timeout(180)
    public void testSingleDroneStateMachineWithFaults() {
        int schedulerPort = test2SchedulerPort;

        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(DroneStateMachineTest.class.getResourceAsStream("/stateMachineTestIncidentFileWithFaults.csv"));
            parser.parseZoneFile(DroneStateMachineTest.class.getResourceAsStream("/zoneLocationsForTesting.csv"));
        } catch (IOException e) {
            fail("Failed to parse incident file or zone location file, aborting. Exception" + e);
        }

        // Create a fresh Scheduler and Drone for each test
        try {
            scheduler = new Scheduler(parser.getZones(), schedulerPort);
            fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);

            // Faults from the drone_faults.csv file
            drone = new TestableDrone("drone1", schedulerAddress, schedulerPort, parser.getZoneMap(),
                    "drone_faults.csv");
        } catch (IOException e) {
            fail("Failed to create scheduler, fire incident subsystem, or drone with Exception: " + e);
        }

        // Create threads for the subsystems
        Thread fiSubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread droneThread = new Thread(drone, "Drone");

        // Nozzle Jam State removed (was below dropping foam)
        ArrayList<Class<? extends DroneState>> expectedStatesInOrder = new ArrayList<>();
        expectedStatesInOrder.add(DroneIdle.class);
        expectedStatesInOrder.add(DroneEnRoute.class);
        expectedStatesInOrder.add(DroneInZone.class);
        expectedStatesInOrder.add(DroneDroppingFoam.class);
        expectedStatesInOrder.add(DroneReturning.class);
        expectedStatesInOrder.add(DroneIdle.class);
        expectedStatesInOrder.add(DroneEnRoute.class);
        expectedStatesInOrder.add(DroneStuck.class);


        // Schedule the drone thread, which will request events when running
        DroneState currentState = drone.getState();
        assertEquals(expectedStatesInOrder.getFirst(), currentState.getClass());
        droneThread.start();

        // Start the simulation which will send an event to the drone and change its
        // state
        fiSubsystemThread.start();

        // Wait for the threads to finish
        try {
            fiSubsystemThread.join();
            droneThread.join();
        } catch (InterruptedException e) {
            fail("Thread interrupted while waiting for completion. Exception: " + e);
        }

        System.out.println("Drone state history with faults:");
        for (Class<? extends DroneState> s : drone.getStateHistory()) {
            System.out.println(s);
        }

        // Ensure the drone's state history matches the expected states
        try {
            for (int i = 1; i < expectedStatesInOrder.size(); i++) {
                assertEquals(expectedStatesInOrder.get(i), drone.getStateHistory().get(i),
                        "Drone state history does not match expected state at index " + i);
            }
        } catch (IndexOutOfBoundsException e) {
            fail("Drone state history does not match expected state history. Exception: " + e);
        }
    }
}
