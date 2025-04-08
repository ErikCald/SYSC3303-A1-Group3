package sysc3303.a1.group3;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


public class Main_fisubsystem {

    public static void main(String[] args) {

        // Load incident and zone files from resources.
        InputStream incidentFile = Main.class.getResourceAsStream("/incident_File.csv");
        InputStream zoneFile = Main.class.getResourceAsStream("/zone_location.csv");
        String schedulerAddress = "localhost"; // Scheduler's IP

        int schedulerPort = 6002; // Scheduler's port

        Parser parser = new Parser();
        try {
            parser.parseIncidentFile(incidentFile);
            parser.parseZoneFile(zoneFile);
        } catch (IOException e) {
            System.err.println("Failed to parse incident_File.csv or zone_location.csv, aborting.");
            e.printStackTrace();
            return;
        }

        List<Zone> zones = parser.getZones();
        Map<Integer, Zone> zoneMap = parser.getZoneMap();

        // Create Fire Incident Subsystem
        FireIncidentSubsystem fiSubsystem = new FireIncidentSubsystem(parser.getEvents(), schedulerAddress, schedulerPort);

        // Create thread for Fire Incident Subsystem
        Thread FIsubsystemThread = new Thread(fiSubsystem, "FireIncidentSubsystem");

        // Start Fire Incident Subsystem
        FIsubsystemThread.start();

    }
}
