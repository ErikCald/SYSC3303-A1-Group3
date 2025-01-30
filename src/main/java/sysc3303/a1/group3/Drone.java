package sysc3303.a1.group3;

public class Drone implements Runnable {

    private final Scheduler scheduler;
    // package private for testing purposes
    Event currentEvent;

    public Drone(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    //Start the Drone, wait for notifications.
    @Override
    public void run() {
        while (!(scheduler.getShutOff())) {
            requestEvent();
            System.out.println("Drone has been scheduled with event: \n" + currentEvent);
            System.out.println("Sending back confirmation to Fire Incident Subsystem.\n");
            scheduler.confirmWithSubsystem(currentEvent);
        }
        System.out.println(Thread.currentThread().getName() + " is shutting down.");
    }

    // package private for testing purposes
    public void requestEvent() {
        currentEvent = scheduler.removeEvent();
    }
}
