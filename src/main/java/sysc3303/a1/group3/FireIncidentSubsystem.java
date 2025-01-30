package sysc3303.a1.group3;

import java.util.ArrayList;
import java.io.InputStream;


/**
 * Represents the Fire Incident Subsystem, which simulates fire incidents and communicates with the Scheduler.
 * This subsystem reads fire incident events from an input file, sends them to the Scheduler, and waits for
 * acknowledgments.
 */
class FireIncidentSubsystem implements Runnable {

    private Scheduler scheduler;
    private ArrayList<Event> events;
    private int eventCount;
    private Parser parser;

    public FireIncidentSubsystem(Scheduler s) {
        this.scheduler = s;
        eventCount = 0;
        events = new ArrayList<>();
        parser = new Parser();

    }

    // Start the subsystem
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

            // Wait for testing purposes. Can be omitted
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("All events have been exhausted, " + Thread.currentThread().getName() + " , is closing.");
    }

    // Use an instance of Parser to parse and add events to the subsystem
    public void parseEvents(){
        InputStream fileStream = Main.class.getResourceAsStream("/incidentFile.csv");
        if(fileStream == null) {
            System.out.println("File doesn't exist");
            return;
        }

        parser = new Parser();
        events = parser.parseIncidientFile(fileStream);
    }

    private synchronized void sendShutOffSignal(){
        scheduler.shutOff();
    }


    public void manageResponse(Event event) {
        System.out.println("The drone confirmed it has received the event: " + event + "\n");
    }

    public ArrayList<Event> getEvents() { return events; }

}

