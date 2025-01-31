package sysc3303.a1.group3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import sysc3303.a1.group3.Event.EventType;

/**
 * A class to parse the incident file and zone file.
 */
public class Parser {
    /**
     * Create a new parser.
     */
    public Parser() {
        
    }

    /**
     * Parse the incident file
     * Expects a csv file in the format hh:mm:ss,zone_id,event_type,severity
     *  
     * @param file The file to parse.
     */
    public List<Event> parseIncidentFile(InputStream file) throws IOException {
        List<Event> events = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    events.add(parseIncidentLine(line));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Failed to parse line: " + line, e);
                }
            }
        }

        return events;
    }

    /**
     * Parse a line from the zone file.
     *
     * @param line The line to parse.
     * @return The zone represented by the line.
     * @throws IllegalArgumentException If the line is not parsed correctly.
     */
    private Event parseIncidentLine(String line) throws IllegalArgumentException {
        String[] values = line.split(",");

        if (values.length < 4) {
            throw new IllegalArgumentException("Not enough columns in line: " + line);
        }

        Time time = Time.valueOf(values[0]);
        int zoneId = Integer.parseInt(values[1]);
        EventType eventType = EventType.fromString(values[2]);
        Severity severity = Severity.fromString(values[3]);

        return new Event(time, zoneId, eventType, severity);
    }

    public List<Zone> parseZoneFile(InputStream file) throws IOException {
        List<Zone> zones = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    zones.add(parseZoneFileLine(line));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Failed to parse line: " + line, e);
                }
            }
        }

        return zones;
    }

    /**
     * Parse a line from the zone file.
     *
     * @param line The line to parse.
     * @return The event represented by the line.
     * @throws IllegalArgumentException If the line is not parsed correctly.
     */
    private Zone parseZoneFileLine(String line) throws IllegalArgumentException {
        String[] values = line.split(",");

        if (values.length < 4) {
            throw new IllegalArgumentException("Not enough columns in line: " + line);
        }

        int zoneId = Integer.parseInt(values[0]);
        int startx = Integer.parseInt(values[1]);
        int starty = Integer.parseInt(values[2]);
        int endx = Integer.parseInt(values[3]);
        int endy = Integer.parseInt(values[4]);

        return new Zone(zoneId, startx, starty, endx, endy);
    }
}
