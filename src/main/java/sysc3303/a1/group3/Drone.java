package sysc3303.a1.group3;

public class Drone implements Runnable {

    private final Scheduler scheduler;

    // package private for testing purposes
    Event currentEvent;

    public Drone(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        // todo: mechanism for cleanly stopping the thread

        while (true) {
            requestEvent();
            System.out.println("Received " + currentEvent);
        }
    }

    // package private for testing purposes
    void requestEvent() {
        currentEvent = scheduler.removeFirst();
    }
}
