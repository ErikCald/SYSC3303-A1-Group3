package sysc3303.a1.group3.drone;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class DroneEventParser {
    private List<DroneEvent> events = new ArrayList<>();
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("H:mm:ss");

    public void parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 3) continue;

                Time time = new Time(TIME_FORMAT.parse(parts[0].trim()).getTime());
                String droneId = parts[1].trim();
                String eventType = parts[2].trim();

                events.add(new DroneEvent(time, droneId, eventType));
            }
        } catch (IOException | ParseException e) {
            System.err.println("Error reading drone_faults.csv: " + e.getMessage());
        }
    }

    public List<DroneEvent> getEvents() {
        return events;
    }
}
