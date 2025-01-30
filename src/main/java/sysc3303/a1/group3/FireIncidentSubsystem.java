package sysc3303.a1.group3;

import java.util.ArrayList;
import java.io.InputStream;


/**
 * Represents the Fire Incident Subsystem, which simulates fire incidents and communicates with the Scheduler.
 * This subsystem reads fire incident events from an input file, sends them to the Scheduler, and waits for
 * acknowledgments.
 */
class FireIncidentSubsystem implements Runnable {

    private final Scheduler scheduler;
    private List<Event> events;
    private int eventCount;
    private Parser parser;

    public FireIncidentSubsystem(Scheduler s) {
        this.scheduler = s;
        eventCount = 0;
        events = new ArrayList<>();
        parser = new Parser();

    }

    // Start the subsystem
    @Override
    public void run() {
        for (int i = 0; i < events.size(); i++) {
            Event event = events.get(i);
            ++eventCount;
            System.out.println("Sending Event " + eventCount + " to Scheduler: \n" + event + "\n");
            scheduler.addEvent(event);

            // Shutoff condition can be changed here, and in the loop
            // Notify all drones waiting on scheduler. Not needed right now since there is one drone
            // constantly reading events, but if there are multiple drones in the future, we will need
            // to re-alert them.
            if (i == events.size() - 1) {
                synchronized (scheduler) {
                    sendShutOffSignal();
                    scheduler.notifyAll();
                }
            }

            // I have left it commented out in case the TAs want to slow the output down for demonstration
            // It should not mess the implementation up at this iteration, but I agree it should be removed later.
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }
        System.out.println("All events have been exhausted, " + Thread.currentThread().getName() + " , is closing.");
    }

    // Use an instance of Parser to parse and add events to the subsystem
    public void parseEvents() {
        InputStream fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        if (fileStream == null) {
            System.out.println("File doesn't exist");
            return;
        }

        events = parser.parseIncidientFile(fileStream);
    }

    // Tell the system to shutoff for a graceful termination.
    // Private for security, should only be called after required work in run is complete.
    private synchronized void sendShutOffSignal(){
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

