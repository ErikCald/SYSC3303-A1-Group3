package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.Drone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * NOTE: notifyAll() is used as in the future, not all Drones will be ready to take new Events.
 * We could use notify() for this iteration, but depending on our implementation in future iterations,
 * I feel justified to use notifyAll() instead.
 *
 * Additionally, the disadvantage of using notifyAll() is overhead, but this is not very relevant as
 * the number of drones and schedulers are very small, solidifying the superiority of notifyAll().
 */


public class Scheduler {

    // Temporary Size variable
    private final int MAX_SIZE = 10;

    // Queue to hold Event objects, to be sent to Drone(s)
    private final Queue<Event> droneMessages;
    // Flags for droneMessageQueue status
    private boolean droneMessagesWritable;
    private boolean droneMessagesReadable;

    // Queue to hold Event objects to send back to the Subsystem (confirmation)
    private final Queue<Event> incidentSubsystemQueue;
    // Flags for incidentSubsystemDeque status
    private boolean incidentSubsystemWritable;
    private boolean incidentSubsystemReadable;

    private FireIncidentSubsystem subsystem;
    private final List<Drone> drones;

    private volatile boolean shutoff;

    public Scheduler() {
        this.droneMessages = new ArrayDeque<>();
        this.incidentSubsystemQueue = new ArrayDeque<>();

        this.droneMessagesWritable = true;
        this.droneMessagesReadable = false;
        this.incidentSubsystemWritable = true;
        this.incidentSubsystemReadable = false;

        this.drones = new ArrayList<>();

        shutoff = false;
    }

    // Add the new event to Queue, Called by the Fire Subsystem
    // wait() if full. (size > 10)
    public synchronized void addEvent(Event event) {

        if (shutoff){
            return;
        }
        // If not writable (full), wait().
        while (!droneMessagesWritable) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        if (shutoff){
            return;
        }

        // Add the event to the queue, adjust booleans.
        droneMessages.add(event);
        droneMessagesReadable = true;
        if (droneMessages.size() >= MAX_SIZE) {
            droneMessagesWritable = false;
        }

        notifyAll();
    }

    // Remove the first Event from the Queue, Called by Drone(s)
    // wait() if no events are available (size <= 0)
    // As per iteration 1 instructions, no meaningful scheduling has been implemented.
    public synchronized Event removeEvent() {
        Event event;

        if (shutoff){
            return null;
        }
        // If not readable (empty queue), wait()
        while (!droneMessagesReadable && !shutoff) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        if (shutoff){
            return null;
        }

        // The drone grabs the first event from the Queue, adjust booleans.
        event = droneMessages.remove();
        droneMessagesWritable = true;
        if (droneMessages.isEmpty()) {
            droneMessagesReadable = false; // No more data, so it's not readable
        }

        notifyAll();
        return event;
    }

    public synchronized void confirmWithSubsystem(Event event) {

        while (!incidentSubsystemWritable) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }

        incidentSubsystemQueue.add(event);

        // In the future, there will be code confirming if the queue is full, elc.
        // Right now, we just send a call back, so it is not needed.

        subsystem.manageResponse(incidentSubsystemQueue.remove());

    }


    public void addDrone(Drone drone){ drones.add(drone); }
    public void setSubsystem(FireIncidentSubsystem subsystem){ this.subsystem = subsystem;}

    //shutoff system, all related objects should observe this for a graceful shutoff.
    public boolean getShutOff(){ return shutoff; }

    //synchronized even if the subsystem calls it just to ensure it has a lock when it calls this
    //and won't trigger a "current thread is not owner" error.
    public synchronized void shutOff(){
        this.shutoff = true;
        notifyAll();
    }

}
