package sysc3303.a1.group3;

import java.io.IOException;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.List;


/**
 * Represents the Fire Incident Subsystem, which simulates fire incidents and communicates with the Scheduler.
 * This subsystem reads fire incident events from an input file, sends them to the Scheduler, and waits for
 * acknowledgments.
 */
public class FireIncidentSubsystem implements Runnable {

    private final Scheduler scheduler;
    private List<Event> events;
    private List<Zone> zones;
    private int eventCount;

    public FireIncidentSubsystem(Scheduler s, InputStream fileStream) throws IOException {
        this.scheduler = s;
        eventCount = 0;
        events = new ArrayList<>();

        Parser parser = new Parser();
        if (fileStream == null) {
            System.out.println("Incident file doesn't exist");
            return;
        }
        events = parser.parseIncidentFile(fileStream);

    }

    // Start the subsystem
    @Override
    public void run() {
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            ++eventCount;
            System.out.println("Sending Event " + eventCount + " to Scheduler: \n" + event + "\n");
            scheduler.addEvent(event);

            // I have left it commented out in case the TAs want to slow the output down for demonstration
            // It should not mess the implementation up at this iteration, but I agree it should be removed later.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
        }
        System.out.println("All events have been exhausted, " + Thread.currentThread().getName() + " , is closing.");
        sendShutOffSignal();

        //
    }

    // Tell the system to shutoff for a graceful termination.
    // Private for security, should only be called after required work in run is complete.
    private void sendShutOffSignal(){
        scheduler.shutOff();
    }

    // In future iterations, this will likely be much more complicated, but in this iteration
    // we were told that text confirmation form the originating class is adequate.
    public void manageResponse(Event event) {
        System.out.println("The drone confirmed it has received the event: " + event + "\n");
    }

    // For JUnit, I'm leaving this as public as it's not a big security risk.
    public List<Event> getEvents() { return events; }

}

