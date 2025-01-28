package sysc3303.a1.group3;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

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
    private Queue<Event> droneMessageQueue;
    // Flags for droneMessageQueue status
    private boolean droneWritable;
    private boolean droneReadable;

    // Queue to hold Event objects to send back to the Subsystem (confirmation)
    private Queue<Event> subsystemMessageQueue;
    // Flags for subsystemMessageQueue status
    private boolean subsystemWritable;
    private boolean subsystemReadable;

    // private FireIncidentSubsystem subsystem;
    // private List<Drone> drones;

    public Scheduler() {
        this.droneMessageQueue = new LinkedList<>();
        this.subsystemMessageQueue = new LinkedList<>();

        this.droneWritable = true;
        this.droneReadable = false;
        this.subsystemWritable = true;
        this.subsystemReadable = false;

        //this.drones = new ArrayList<>();

    }

    // Add the new event to Queue, Called by the Fire Subsystem
    // wait() if full. (size > 10)
    public synchronized void addLast(Event event) {

        // If not writable (full), wait().
        while (!droneWritable) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(e);
            }
        }

        // Add the event to the queue, adjust booleans.
        droneMessageQueue.offer(event);
        droneReadable = true;
        if (droneMessageQueue.size() == MAX_SIZE) {
            droneWritable = false;
        }

        notifyAll();
    }

    // Remove the first Event from the Queue, Called by Drone(s)
    // wait() if no events are available (size <= 0)
    // As per iteration 1 instructions, no meaningful scheduling has been implemented.
    public synchronized Event removeFirst() {
        Event event;

        // If not readable (empty queue), wait()
        while (!droneReadable) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(e);
            }
        }

        // The drone grabs the first event from the Queue, adjust booleans.
        event = droneMessageQueue.poll();
        droneWritable = true;
        if (droneMessageQueue.isEmpty()) {
            droneReadable = false; // No more data, so it's not readable
        }

        notifyAll();
        return event;
    }

    public synchronized void confirmWithSubsystem(Event event) {

        while (!subsystemWritable) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(e);
            }
        }

        subsystemMessageQueue.offer(event);
        //subsystem.manageResponse(subsystemMessageQueue.poll());

    }

    /*
    public void addDrone(Drone drone){ drones.add(drone); }
    public void setSubsystem(FireIncidentSubsystem subsystem){ this.subsystem = subsystem;}
     */

}
