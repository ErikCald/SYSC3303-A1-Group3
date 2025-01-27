package sysc3303.a1.group3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.sql.Time;
import java.util.ArrayList;

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
        parser = new Parser();
    }

    /**
     * Test parsing an valid incident file.
     */
    @Test
    public void testParseIncidentFile() {
        InputStream fileStream = ParserTest.class.getResourceAsStream("/validIncidentFileForTesting.csv");
        if(fileStream == null) {
            throw new RuntimeException("Failed to load incident file for testing");
        }

        ArrayList<Event> events = ParserTest.parser.parseIncidientFile(fileStream);

        ArrayList<Event> expectedEvents = new ArrayList<>();
        expectedEvents.add(new Event(Time.valueOf("0:00:05"), 3, EventType.FIRED_DETECTED, Severity.Low));
        expectedEvents.add(new Event(Time.valueOf("0:00:24"), 1, EventType.FIRED_DETECTED, Severity.High));
        expectedEvents.add(new Event(Time.valueOf("0:00:51"), 2, EventType.DRONE_REQUESTED, Severity.Moderate));
        expectedEvents.add(new Event(Time.valueOf("0:01:13"), 4, EventType.FIRED_DETECTED, Severity.Low));
        expectedEvents.add(new Event(Time.valueOf("0:01:31"), 1, EventType.DRONE_REQUESTED, Severity.Moderate));   
        
        assertEquals(expectedEvents.size(), events.size(), "Test file has a mismatching quantity of events with the expected events.");

        for(int i = 0; i < events.size(); ++i) {
            assertEquals(expectedEvents.get(i), events.get(i), "Event " + (i+1) + " does not match the expected event");
        }
    }

    /**
     * Test parsing an invalid incident file.
     */
    @Test
    public void testEmptyFile() {
        InputStream fileStream = ParserTest.class.getResourceAsStream("/emptyFileForTesting.csv");

        ArrayList<Event> events = parser.parseIncidientFile(fileStream);
        assertTrue(events.isEmpty(), "Empty file should return an empty list of events");
    }
}
