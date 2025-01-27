package sysc3303.a1.group3;

import java.util.LinkedList;
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

    // Queue to hold Event objects, to be sent to Drone(s)
    private final Queue<Event> eventQueue;
    // Temporary Size variable
    private final int MAX_SIZE = 10;

    // Flags to determine if the drone/fire subsystem should wait.
    private boolean writable;
    private boolean readable;

    public Scheduler() {
        this.eventQueue = new LinkedList<>();
        this.writable = true;
        this.readable = false;
    }

    // Add the new event to Queue, Called by the Fire Subsystem
    // wait() if full. (size > 10)
    public synchronized void addLast(Event element) {

        // If not writable (full), wait().
        while (!writable) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(e);
            }
        }

        // Add the element to the queue, adjust booleans.
        eventQueue.offer(element);
        readable = true;
        if (eventQueue.size() == MAX_SIZE) {
            writable = false;
        }

        notifyAll();
    }

    // Remove the first Event from the Queue, Called by Drone(s)
    // wait() if no events are available (size <= 0)
    // As per iteration 1 instructions, no meaningful scheduling has been implemented.
    public synchronized Event removeFirst() {
        Event event;

        // If not readable (empty queue), wait()
        while (!readable) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println(e);
            }
        }

        // The drone grabs the first event from the Queue, adjust booleans.
        event = eventQueue.poll();
        writable = true;
        if (eventQueue.isEmpty()) {
            readable = false; // No more data, so it's not readable
        }

        notifyAll();
        return event;
    }
}
