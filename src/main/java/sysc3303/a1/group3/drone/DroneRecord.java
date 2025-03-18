package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;
import sysc3303.a1.group3.physics.Vector2d;

import java.net.InetAddress;

public class DroneRecord {

    private final String droneName;
    private String state;

    private int dronePort;
    private InetAddress droneAddress;
    private int listenerPort;
    private InetAddress listenerAddress;
    private int shutOffPort;
    private InetAddress shutOffAddress;
    private Event event;
    Vector2d position;

    public DroneRecord(String n, String s, double x, double y){
        this.droneName = n;
        this.state = s;

        this.dronePort = -1;
        this.droneAddress = null;

        this.listenerPort = -1;
        this.listenerAddress = null;

        this.event = null;
        position = new Vector2d(x, y);
    }

    public String getDroneName(){ return droneName; }
    public String getState(){ return state; }
    public void setState(String s){ this.state = s; }

    public int getListenerPort(){ return listenerPort; }
    public InetAddress getListenerAddress(){ return listenerAddress; }
    public void setListenerPort(int p){listenerPort = p; }
    public void setListenerAddress(InetAddress a){ listenerAddress = a; }

    public int getDronePort(){ return dronePort; }
    public InetAddress getDroneAddress(){ return droneAddress; }
    public void setDronePort(int p){dronePort = p; }
    public void setDroneAddress(InetAddress a){ droneAddress = a; }

    public int getShutOffPort(){ return shutOffPort; }
    public InetAddress getShutOffAddress(){ return shutOffAddress; }
    public void setShutOffPort(int p){shutOffPort = p; }
    public void setShutOffAddress(InetAddress a){ shutOffAddress = a; }

    public Event getEvent(){ return event; }
    public void setEvent(Event e){ this.event = e; }
    public Vector2d getPosition(){ return position; }
    public void setPosition(Vector2d newPosition){
        this.position = newPosition;
    }

}
