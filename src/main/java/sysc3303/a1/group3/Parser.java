package sysc3303.a1.group3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;

import sysc3303.a1.group3.Event.EventType;

public class Parser {

    public static void main(String[] args) {
        Parser parser = new Parser();
        ArrayList<Event> events = parser.parseIncidientFile("C:\\Users\\eeejj\\Documents\\Code\\University\\Winter2025\\SYSC3303\\GroupProject\\src\\main\\java\\sysc3303\\a1\\group3\\incidentsReportTest.csv");

        for(Event event : events) {
            System.out.println(event);
        }
    }

    public Parser() {

    }

    private boolean isFilenameValid(String filename) {
        return filename == null 
            || filename.isEmpty() 
            || filename.endsWith(".csv");
    }


    // Time,Zone_ID,Event_type,Severity
    // 0:00:05,3,FIRE_DETECTED,Low
    // 0:00:24,1,FiRE_DETECTED,High
    // 0:00:51,2,DRONE_REQUEST,Moderate
    // 0:01:13,4,FiRE_DETECTED,Low
    // 0:01:31,1,DRONE_REQUEST,Moderate

    public ArrayList<Event> parseIncidientFile(String filename) {
        if(!isFilenameValid(filename)) {
            throw new RuntimeException("Invalid file given to parseIncident");
        }

        ArrayList<Event> events = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    events.add(parseIncidentLine(line));
                } catch (IllegalArgumentException e) {
                    System.out.printf("Error parsing line: %s with exception: %s, continuing to next line.%n", line, e);
                    continue;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + filename + ", with exception: " + e);
        }

        return events;
    }

    /**
     * Parse a line from the incident file.
     * 
     * @param line The line to parse.
     * @return The event represented by the line.
     * @throws IllegalArgumentException If the line is not parsed correctly.
     */
    private Event parseIncidentLine(String line) throws IllegalArgumentException {
        String[] values = line.split(",");

        if(values.length < 4) {
            throw new IllegalArgumentException("Parse Error: Not enough coloums in line: " + line);
        }

        try {
            Time time = Time.valueOf(values[0]);
            int zoneId = Integer.parseInt(values[1]);
            EventType eventType = EventType.fromString(values[2]);
            Severity severity = Severity.fromString(values[3]);

            return new Event(time, zoneId, eventType, severity);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid line: " + line);
        }
    }
}
