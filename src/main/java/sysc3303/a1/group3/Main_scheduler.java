package sysc3303.a1.group3;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Main_scheduler {
    public static final long START_TIME = System.currentTimeMillis();
    private static Thread.State DroneIdle;

    public static void main(String[] args) {

        // Load incident and zone files from resources.
        InputStream incidentFile = Main.class.getResourceAsStream("/incidentFile.csv");
        InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");
        String schedulerAddress = "localhost"; // Scheduler's IP

        int schedulerPort = 6002; // Scheduler's port

        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(incidentFile);
            parser.parseZoneFile(zoneFile);
        } catch (IOException e) {
            System.err.println("Failed to parse incidentFile.csv or zone_location.csv, aborting.");
            e.printStackTrace();
            return;
        }

        List<Zone> zones = parser.getZones();
        Map<Integer, Zone> zoneMap = parser.getZoneMap();


        Scheduler scheduler;
        try {
            scheduler = new Scheduler(zones, schedulerPort);
        } catch (IOException e) {
            System.err.println("Failed to create scheduler, aborting.");
            e.printStackTrace();
            return;
        }
    }
}
