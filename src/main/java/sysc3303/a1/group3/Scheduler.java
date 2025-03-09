package sysc3303.a1.group3;

import sysc3303.a1.group3.drone.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLOutput;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


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
    private final List<Zone> zones;

    private volatile boolean shutoff;

    private DatagramSocket socket;

    //Constructor with no Zones (Iteration 1)
    public Scheduler() {
        this.droneMessages = new ArrayDeque<>();
        this.incidentSubsystemQueue = new ArrayDeque<>();

        this.droneMessagesWritable = true;
        this.droneMessagesReadable = false;
        this.incidentSubsystemWritable = true;
        this.incidentSubsystemReadable = false;

        this.drones = new ArrayList<>();
        this.zones = new ArrayList<>();

        shutoff = false;

        try {
            this.socket = new DatagramSocket(schedulerPort);
            receiveEvents();
            receiveDroneStates();
        } catch (SocketException e) {
            System.err.println("Error creating socket: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public Scheduler(InputStream zoneFile) throws IOException {
        this.droneMessages = new ArrayDeque<>();
        this.incidentSubsystemQueue = new ArrayDeque<>();

        this.droneMessagesWritable = true;
        this.droneMessagesReadable = false;
        this.incidentSubsystemWritable = true;
        this.incidentSubsystemReadable = false;

        this.drones = new ArrayList<>();

        shutoff = false;

        Parser parser = new Parser();
        if (zoneFile == null) {
            zones = new ArrayList<>();
            System.out.println("Zone file doesn't exist");
            return;
        }
        zones = parser.parseZoneFile(zoneFile);
    }

    //New Method
    private void receiveEvents() {
        try {
            byte[] receiveData = new byte[1024];
            while (!shutoff) {
                DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(packet);
                String eventData = new String(packet.getData(), 0, packet.getLength());
                Event event = convertJsonToEvent(eventData); // Implement this method
                addEvent(event); // Add to scheduler's queue
            }
        } catch (IOException e) {
            System.err.println("Error receiving events: " + e.getMessage());
        }
    }
    //New Method
    private Event convertJsonToEvent(String eventData) {
        // Implement JSON deserialization here (e.g., using Gson library)
        // Create and return Event object from JSON
        return null; // Replace with actual implementation
    }
    //New Method
    private void receiveDroneStates() {
        new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (!shutoff) {
                    DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(packet);
                    String droneState = new String(packet.getData(), 0, packet.getLength());

                    // Process drone state data
                    processDroneState(droneState);

                }
            } catch (IOException e) {
                System.err.println("Error receiving drone state: " + e.getMessage());
            }
        }).start();
    }
    //New Method
    private void processDroneState(String droneState) {
        //Implement this method
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
    // As per iteration 1 instruction, no meaningful scheduling has been implemented.
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

        if (droneMessages.isEmpty()){
            return null;
        }

        // The drone grabs the first event from the Queue, adjust booleans.
        // Then send the event through the scheduling algorithm for distribution

        List<Drone> availableDrones = getAvailableDrones();
        // If all drones already have an event, then this call is likely leftover from a drone calling for a new event
        // when distributeEvent gave it one.
        // Thus, if all drones have events already, but removeEvent is called, then ignore the call.
        if (availableDrones.isEmpty()){
            return null;
        }
        event = droneMessages.remove();
        distributeEvent(event, availableDrones);

        droneMessagesWritable = true;
        if (droneMessages.isEmpty()) {
            droneMessagesReadable = false; // No more data, so it's not readable
        }

        notifyAll();
        return event;
    }

    // Scheduling Algorithm:
    // If there is only 1 drone available, then we assign that drone the event as you need to have no event to ask for one.
    // If there are multiple drones available, then give the event to the drone closes to the Zone.
        // If that Drone already had an event, redistribute the event to the rest of the drones recursively, excluding the initial drone.
        // Later, we can modify availableDrones to only have drones who have enough water.
    // However, if there are multiple drones available, but they all have events, then wait.
    private void distributeEvent(Event event, List<Drone> availableDrones) {
        if (availableDrones.isEmpty()) {
            System.out.println("No available drones to assign the event. Something has gone wrong!");
            return;
        }

        Drone selectedDrone = null;
        Event previousEvent = null;

        if (availableDrones.size() == 1) {
            // If there's only one available drone, assign the event to that drone
            // This means this is the drone that just asked for an Event, which means it shouldn't have one.
            // So, just assign it and return.
            selectedDrone = availableDrones.getFirst();
            if (selectedDrone.getCurrentEvent() != null) {
                System.out.println("event: " + selectedDrone.getCurrentEvent());
                System.err.println("ERROR: Did a drone just ask for an event, but it already have one? Or some other error?");
                throw new IllegalStateException("Drone " + selectedDrone + " is marked as available but already has an event: "
                    + selectedDrone.getCurrentEvent() + " ... Something messed up!");
            }
        } else {
            // If multiple drones are available, find the one closest to the zone
            int minDistance = Integer.MAX_VALUE;
            for (Drone drone : availableDrones) {
                int distance = getDistanceFromZone(drone, event);
                // System.out.println("Drone " + drone + " distance: " + distance); // Debugging
                if (distance < minDistance) {
                    minDistance = distance;
                    selectedDrone = drone;
                }
            }
        }

        if (selectedDrone != null) {
            // Check if the selected drone already had an event
            if (selectedDrone.getCurrentEvent() != null) {
                previousEvent = selectedDrone.getCurrentEvent();
            }

            // Assign the new event to the selected drone either way
            selectedDrone.setCurrentEvent(event);
            //System.out.println("Assigned event to Drone " + selectedDrone.getName()); //debugging

            // Redistribute the previous event recursively (if there was one)
            // Be sure to exclude the drone that was just selected!
            if (previousEvent != null) {
                List<Drone> updatedAvailableDrones = new ArrayList<>(availableDrones);
                updatedAvailableDrones.remove(selectedDrone);
                distributeEvent(previousEvent, updatedAvailableDrones);
            }
        } else {
            System.out.println("No available drone without an event.");
        }
        System.out.println(selectedDrone.getName() + " is scheduled with event, " + event);
    }



    // Gets a list of all drones that are Idle or EnRoute.
    // We can add further criteria later.
    private List<Drone> getAvailableDrones(){
        List<Drone> availableDrones = new ArrayList<>();
        Boolean allHaveEvents = true;

        for (Drone drone : drones) {
            // Check if the drone's state is either DroneIdle or DroneEnRoute.
            // This can be changed later easily if we want to modify the selection of Drones.
            if (drone.getState() instanceof DroneIdle || drone.getState() instanceof DroneEnRoute) {
                availableDrones.add(drone);
                if (drone.getCurrentEvent() == null){
                    allHaveEvents = false;
                }
            }
        }

        // If all the available Drones have events, then tell the scheduler to wait instead.
        if (allHaveEvents){
            return new ArrayList<>();
        } else {
            return availableDrones;
        }
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

    //Should be changed when movement and location for drones is implemented.
    //Gets the distance from the zone to the event's zone
    private int getDistanceFromZone(Drone drone, Event event) {
        // Find the Zone corresponding to the event's zoneId
        Zone zone = null;
        for (Zone z : zones) {
            if (z.zoneID() == event.getZoneId()) {
                zone = z;
                break;
            }
        }

        // If no zone is found...
        if (zone == null) {
            throw new IllegalArgumentException("Zone not found for zoneId: " + event.getZoneId());
        }

        // Get the center of the zone and position of drone:
        int[] zoneCenter = getCenter(zone);
        int[] dronePosition = drone.getPosition();
        int centerX = zoneCenter[0];
        int centerY = zoneCenter[1];
        int droneX = dronePosition[0];
        int droneY = dronePosition[1];

        // Calculate the distance between the drone and the zone center.
        // See "Euclidean distance" (https://en.wikipedia.org/wiki/Euclidean_distance)
        // redundant variable declaration for debug
        int distance = (int) Math.sqrt(Math.pow(centerX - droneX, 2) + Math.pow(centerY - droneY, 2));

        return distance;
    }


    //Get the x and y of the center of the zone.
    //We could move this later, but only Scheduler needs it right now.
    public static int[] getCenter(Zone zone) {
        int centerX = (zone.start_x() + zone.end_x()) / 2;
        int centerY = (zone.start_y() + zone.end_y()) / 2;

        return new int[] { centerX, centerY };
    }

    public void addDrone(Drone drone){ drones.add(drone); }
    public void setSubsystem(FireIncidentSubsystem subsystem){ this.subsystem = subsystem;}

    public boolean confirmDroneInZone(Drone drone){
        //Later, the state of the drone can be used to determine what it should do.
        //With just one drone, it should just start extinguishing the fire.
        return true;
    }

    //shutoff system, all related objects should observe this for a graceful shutoff.
    public boolean getShutOff(){ return shutoff; }

    //synchronized even if the subsystem calls it just to ensure it has a lock when it calls this
    //and won't trigger a "current thread is not owner" error.
    public synchronized void shutOff(){
        while (!droneMessages.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
        this.shutoff = true;
        notifyAll();
    }

}
