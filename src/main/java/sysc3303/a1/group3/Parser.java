package sysc3303.a1.group3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import sysc3303.a1.group3.Event.EventType;

/**
 * A class to parse incident and zone files.
 */
public class Parser {
    private List<Event> events;
    private List<Zone> zones;
    
    public Parser() {
        events = new ArrayList<>();
        zones = new ArrayList<>();
    }

    public List<Event> getEvents() {
        if (events.isEmpty()) {
            throw new IllegalStateException("No events have been parsed yet.");
        }
        return events;
    }
     
    public List<Zone> getZones() {
        if (zones.isEmpty()) {
            throw new IllegalStateException("No zones have been parsed yet.");
        }
        return zones;
    }

    public Map<Integer, Zone> getZoneMap() {
        return zones.stream()
            .collect(Collectors.toMap(Zone::zoneID, z -> z));
    }

    public List<Event> parseIncidentFile(InputStream file) throws IOException {
        events = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file))) {

            br.readLine(); // skip header -- called separately to avoid erroneous warning.

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

    private Event parseIncidentLine(String line) throws IllegalArgumentException {
        String[] values = line.split(",");
        if (values.length < 4) {
            throw new IllegalArgumentException("Not enough columns in line: " + line);
        }
        Time time = Time.valueOf(values[0]);

        // Extract hours, minutes, and seconds
        int hours = time.getHours();
        int minutes = time.getMinutes();
        int seconds = time.getSeconds();

        // Convert to seconds, and divide/adjust:
        int totalSeconds = hours * 3600 + minutes * 60 + seconds;
        int adjustedTotalSeconds = totalSeconds / 10;
        int adjustedHours = adjustedTotalSeconds / 3600;
        int adjustedMinutes = (adjustedTotalSeconds % 3600) / 60;
        int adjustedSeconds = adjustedTotalSeconds % 60;

        // New time object
        Time adjustedTime = new Time(adjustedHours, adjustedMinutes, adjustedSeconds);

        int zoneId = Integer.parseInt(values[1]);
        EventType eventType = EventType.fromString(values[2]);
        Severity severity = Severity.fromString(values[3]);

        return new Event(adjustedTime , zoneId, eventType, severity);
    }

    public List<Zone> parseZoneFile(InputStream file) throws IOException {
        zones = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file))) {

            br.readLine(); // skip header -- called separately to avoid erroneous warning.

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

    private Zone parseZoneFileLine(String line) throws IllegalArgumentException {
        String[] values = line.split(",");
        if (values.length < 5) {
            throw new IllegalArgumentException("Not enough columns in line: " + line);
        }
        int zoneId = Integer.parseInt(values[0]);
        int startx = Integer.parseInt(values[1]);
        int starty = Integer.parseInt(values[2]);
        int endx = Integer.parseInt(values[3]);
        int endy = Integer.parseInt(values[4]);
        return Zone.of(zoneId, startx, starty, endx, endy);
    }
}
