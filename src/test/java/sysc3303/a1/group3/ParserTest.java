package sysc3303.a1.group3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import sysc3303.a1.group3.Event.EventType;

/**
 * Tests for the Parser class.
 */
class ParserTest {
    private static Parser parser;

    @BeforeAll
    public static void setUp() {
        UI.setIsUIDisabled(true); // Disable UI for testing
        parser = new Parser();
    }

    /**
     * Test parsing an valid incident file.
     */
    @Test
    public void testParseIncidentFile() throws IOException {
        // Load the incident file for testing
        InputStream fileStream = ParserTest.class.getResourceAsStream("/validIncidentFileForTesting.csv");
        if (fileStream == null) {
            throw new RuntimeException("Failed to load incident file for testing");
        }
        // Parse the incident file
        List<Event> events = parser.parseIncidentFile(fileStream);

        // Define the expected events
        List<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(new Event(Time.valueOf("00:00:03"), 3, EventType.FIRE_DETECTED, Severity.Low));
        expectedEvents.add(new Event(Time.valueOf("00:00:04"), 1, EventType.FIRE_DETECTED, Severity.High));
        expectedEvents.add(new Event(Time.valueOf("00:00:15"), 2, EventType.DRONE_REQUESTED, Severity.Moderate));
        expectedEvents.add(new Event(Time.valueOf("00:00:20"), 4, EventType.FIRE_DETECTED, Severity.Low));
        expectedEvents.add(new Event(Time.valueOf("00:00:25"), 1, EventType.DRONE_REQUESTED, Severity.Moderate));


        // Assert that the number of events parsed matches the expected count
        assertEquals(expectedEvents.size(), events.size(), "Test file has a mismatching quantity of events with the expected events.");

        // Assert that each parsed event matches the expected event
        for (int i = 0; i < events.size(); ++i) {
            assertEquals(expectedEvents.get(i), events.get(i), "Event " + (i + 1) + " does not match the expected event");
        }
    }

    /**
     * Test parsing an invalid incident file.
     */
    @Test
    public void testEmptyFile() throws IOException {
        InputStream fileStream = ParserTest.class.getResourceAsStream("/emptyFileForTesting.csv");

        List<Event> events = parser.parseIncidentFile(fileStream);
        assertTrue(events.isEmpty(), "Empty file should return an empty list of events");
    }
}
