package sysc3303.a1.group3;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.LinkedList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import sysc3303.a1.group3.drone.Drone;

public class WholeSystemTest {

    private Scheduler scheduler;
    private FireIncidentSubsystem fiSubsystem;
    private Drone drone1;
    private Drone drone2;

    private InputStream fileStream;
    private InputStream zoneFile;

    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void beforeEach() throws IOException {
        System.setOut(new PrintStream(outContent));

        fileStream = WholeSystemTest.class.getResourceAsStream("/wholeSystemTestIncidentFile.csv");
        zoneFile = WholeSystemTest.class.getResourceAsStream("/zoneLocationsForTesting.csv");

        scheduler = new Scheduler(zoneFile);
        fiSubsystem = new FireIncidentSubsystem(scheduler, fileStream);
        drone1 = new Drone("Drone1", scheduler);
        drone2 = new Drone("Drone2", scheduler);
        scheduler.setSubsystem(fiSubsystem);
    }

    @AfterEach
    public void afterEach() {
        System.setOut(originalOut);
    }

    // Test the whole system, similar to main
    @Test
    @Timeout(60)
    public void singleDroneSystemTest() {
        scheduler.addDrone(drone1);

        // Create threads for the subsystems
        Thread fiSubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread drone1Thread = new Thread(drone1, "Drone1");

        // Start the simulation
        fiSubsystemThread.start();
        drone1Thread.start();

        // Wait for threads to be finished.
        try {
            fiSubsystemThread.join();
            drone1Thread.join();
        } catch (InterruptedException e) {
            fail("Thread was interrupted prematurely.");
            throw new RuntimeException(e);
        }

        // Convert the recorded output stream to a list of strings
        LinkedList<String> printedStrings = new LinkedList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(outContent.toString()))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                printedStrings.addLast(line);
            }
        } catch (IOException e) {
            fail("Failed to convert output stream to string.");
            throw new RuntimeException(e);
        }

        // Validate the output
        String[] expectedLines = {
            "Sending Event 1 to Scheduler:",
            "Event: 00:00:01, Zone: 3, Type: FIRED_DETECTED, Severity: Low",
            "",
            "Drone1 is scheduled with event, Event: 00:00:01, Zone: 3, Type: FIRED_DETECTED, Severity: Low",
            "Drone1 on the way!",
            "Drone1 is extinguishing flames!",
            "the drone releases some water.",
            "Drone1 returning!",
            "Drone1 is back!",
            "Sending Event 2 to Scheduler:",
            "Event: 00:00:05, Zone: 2, Type: DRONE_REQUESTED, Severity: Low",
            "",
            "Drone1 is scheduled with event, Event: 00:00:05, Zone: 2, Type: DRONE_REQUESTED, Severity: Low",
            "Drone1 on the way!",
            "All events have been exhausted, FireIncidentSubsystem, is closing.",
            "Drone1 is extinguishing flames!",
            "the drone releases some water.",
            "Drone1 returning!",
            "Drone1 is back!",
            "Drone1 is shutting down."
        };

        for (int i = 0; i < expectedLines.length; i++) {
            if (i >= printedStrings.size()) {
                fail("More strings expected than printed. Expected: " + expectedLines[i] + "\nActual: \"\"");
            } else if (!expectedLines[i].equals(printedStrings.get(i).trim())) {
                fail("Line " + (i + 1) + " does not match the expected output. \nExpected: " + expectedLines[i] + "\nActual: " + printedStrings.get(i));
            }
        }
        if (printedStrings.size() > expectedLines.length) {
            fail("More strings printed than expected. Next Expected String: " + printedStrings.get(expectedLines.length));
        }
    }

    /**
     * Check if the target string matches the string at the index of the array, or the string at the index - 1 or index + 1.
     * 
     * @param target The target string to match.
     * @param array The array of strings to check.
     * @param index The index of the array to check.
     * @return Boolean condition for matching the target string.
     */
    public boolean checkStringFuzzyMatch(String target, String[] array, int index) {
        for(int i = index - 2; i <= index + 2; i++) {
            if(i >= 0 && i < array.length) {
                if(array[i].equals(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    @Timeout(180)
    public void twoDroneSystemTest() {
        scheduler.addDrone(drone1);
        scheduler.addDrone(drone2);

        // Create threads for the subsystems
        Thread fiSubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");
        Thread drone1Thread = new Thread(drone1, "Drone1");
        Thread drone2Thread = new Thread(drone2, "Drone2");

        // Start the simulation
        fiSubsystemThread.start();
        drone1Thread.start();
        drone2Thread.start();

        // Wait for threads to be finished.
        try {
            fiSubsystemThread.join();
            drone1Thread.join();
            drone2Thread.join();
        } catch (InterruptedException e) {
            fail("Thread was interrupted prematurely.");
            throw new RuntimeException(e);
        }

        // Convert the recorded output stream to a list of strings
        LinkedList<String> printedStrings = new LinkedList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new StringReader(outContent.toString()))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                printedStrings.addLast(line);
            }
        } catch (IOException e) {
            fail("Failed to convert output stream to string.");
            throw new RuntimeException(e);
        }

//        afterAll();
        System.setOut(originalOut);
        System.out.println("\n\nOutput:");
        System.out.println(outContent.toString());
        System.out.println("\n\nPrintedStrings:");
        System.out.println(printedStrings);

        // Validate the output
        String[] expectedLines = {
            "Sending Event 1 to Scheduler:",
            "Event: 00:00:01, Zone: 3, Type: FIRED_DETECTED, Severity: Low",
            "",
            "Drone2 is scheduled with event, Event: 00:00:01, Zone: 3, Type: FIRED_DETECTED, Severity: Low",
            "Sending Event 2 to Scheduler:",
            "Event: 00:00:05, Zone: 2, Type: DRONE_REQUESTED, Severity: Low",
            "",
            "All events have been exhausted, FireIncidentSubsystem, is closing.",
            "Drone1 is scheduled with event, Event: 00:00:01, Zone: 3, Type: FIRED_DETECTED, Severity: Low",
            "Drone2 is scheduled with event, Event: 00:00:05, Zone: 2, Type: DRONE_REQUESTED, Severity: Low",
            "Drone1 on the way!",
            "Drone2 on the way!",
            "Drone1 is extinguishing flames!",
            "Drone2 is extinguishing flames!",
            "the drone releases some water.",
            "Drone2 returning!",
            "the drone releases some water.",
            "Drone1 returning!",
            "Drone1 is back!",
            "Drone2 is back!",
            "Drone2 is shutting down.",
            "Drone1 is shutting down.",
        };

        for (int i = 0; i < expectedLines.length; i++) {
            if (i >= printedStrings.size()) {
                fail("More strings expected than printed. Expected: " + expectedLines[i] + "\nActual: \"\"");
            } else if (!checkStringFuzzyMatch(printedStrings.get(i).trim(), expectedLines, i)) {
                fail("Line " + (i + 1) + " does not match the expected output. \nExpected: " + expectedLines[i] + "\nActual: " + printedStrings.get(i));
            }
        }
        if (printedStrings.size() > expectedLines.length) {
            fail("More strings printed than expected. Next Expected String: " + printedStrings.get(expectedLines.length));
        }
    }

}
