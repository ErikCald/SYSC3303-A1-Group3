package sysc3303.a1.group3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sysc3303.a1.group3.drone.Drone;
import sysc3303.a1.group3.drone.DroneDroppingFoam;
import sysc3303.a1.group3.drone.DroneEnRoute;
import sysc3303.a1.group3.drone.DroneIdle;
import sysc3303.a1.group3.drone.DroneInZone;
import sysc3303.a1.group3.drone.DroneReturning;
import sysc3303.a1.group3.drone.DroneState;

public class StateMachineTest {

    /**
     * A testable version of the Drone class that allows for the onStateChange callback to be set.
     */
    public class TestableDrone extends Drone {
        public TestableDrone(String name, Scheduler scheduler) {
            super(name, scheduler);
            stateHistory.add(DroneIdle.class);
        }

        ArrayList<Class<? extends DroneState>> stateHistory = new ArrayList<>();

        @Override
        public void transitionState(Class<? extends DroneState> state) {
            stateHistory.add(state);
            try {
                super.transitionState(state);
            } catch (InterruptedException e) {
                fail("Transitioning state failed");
            }
        }

        public ArrayList<Class<? extends DroneState>> getStateHistory() {
            return stateHistory;
        }
    }

    private Scheduler scheduler;
    private TestableDrone drone;
    private FireIncidentSubsystem fiSubsystem;
    private InputStream fileStream;

    @BeforeEach
    void beforeEach() throws IOException {
        // Create a fresh Scheduler and Drone for each test
        fileStream = StateMachineTest.class.getResourceAsStream("/stateMachineTestIncidentFile.csv");
        scheduler = new Scheduler();
        drone = new TestableDrone("drone", scheduler);
        fiSubsystem = new FireIncidentSubsystem(scheduler, fileStream);
        scheduler.addDrone(drone);
        scheduler.setSubsystem(fiSubsystem);
    }

    @Test
    public void testSingleDroneStateMachine() {
        // Create threads for the subsystems
        Thread fiSubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread droneThread = new Thread(drone, "Drone");

        ArrayList<Class<? extends DroneState>> expectedStatesInOrder = new ArrayList<>();
        expectedStatesInOrder.add(DroneIdle.class);
        expectedStatesInOrder.add(DroneEnRoute.class);
        expectedStatesInOrder.add(DroneInZone.class);
        expectedStatesInOrder.add(DroneDroppingFoam.class);
        expectedStatesInOrder.add(DroneReturning.class);
        expectedStatesInOrder.add(DroneIdle.class);

        // Schedule the drone thread, which will request events when running
        DroneState currentState = drone.getState();
        assertEquals(expectedStatesInOrder.getFirst(), currentState.getClass());
        droneThread.start();

        // Start the simulation which will send a event to the drone and change its state
        fiSubsystemThread.start();
      
        // Wait for the threads to finish
        try {
            fiSubsystemThread.join();
            droneThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Ensure the drone's state history matches the expected states
        for (int i = 1; i < expectedStatesInOrder.size(); i++) {
            assertEquals(expectedStatesInOrder.get(i), drone.getStateHistory().get(i), "Drone state history does not match expected state at index " + i);
        }
    }
}
