package sysc3303.a1.group3;

import java.io.InputStream;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        // Example usage of the Parser class. Can remove this whenever.
        InputStream fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        if(fileStream == null) {
            System.out.println("File doesn't exist");
            return;
        }

        Parser parser = new Parser();
        ArrayList<Event> events = parser.parseIncidientFile(fileStream);
        for(Event e : events) {
            System.out.println(e);
        }
    }
}
