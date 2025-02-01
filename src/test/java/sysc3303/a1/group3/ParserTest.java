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
        parser = new Parser();
    }

    /**
     * Test parsing an valid incident file.
     */
    @Test
    public void testParseIncidentFile() throws IOException {
        InputStream fileStream = ParserTest.class.getResourceAsStream("/validIncidentFileForTesting.csv");
        if(fileStream == null) {
            throw new RuntimeException("Failed to load incident file for testing");
        }

        List<Event> events = ParserTest.parser.parseIncidentFile(fileStream);

        List<Event> expectedEvents = new ArrayList<>();
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
    public void testEmptyFile() throws IOException {
        InputStream fileStream = ParserTest.class.getResourceAsStream("/emptyFileForTesting.csv");

        List<Event> events = parser.parseIncidentFile(fileStream);
        assertTrue(events.isEmpty(), "Empty file should return an empty list of events");
    }
}
