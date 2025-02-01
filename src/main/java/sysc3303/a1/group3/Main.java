package sysc3303.a1.group3;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        // Example usage of the Parser class. Can remove this whenever.
        InputStream fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        if(fileStream == null) {
            System.out.println("File doesn't exist");
            return;
        }

        Parser parser = new Parser();
        List<Event> events;
        try {
            events = parser.parseIncidentFile(fileStream);
        } catch (IOException e) {
            System.err.println("Failed to parse incidentFile.csv");
            e.printStackTrace();
            return;
        }

        for (Event e : events) {
            System.out.println(e);
        }
    }
}
