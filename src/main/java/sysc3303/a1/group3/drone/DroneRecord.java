package sysc3303.a1.group3.drone;

import sysc3303.a1.group3.Event;

import java.net.InetAddress;

public class DroneRecord {

    private final String droneName;
    private String state;

    private int dronePort;
    private InetAddress droneAddress;
    private int listenerPort;
    private InetAddress listenerAddress;
    private Event event;
    private double x,y;

    public DroneRecord(String n, String s, double x, double y){
        this.droneName = n;
        this.state = s;

        this.dronePort = -1;
        this.droneAddress = null;

        this.listenerPort = -1;
        this.listenerAddress = null;

        this.event = null;
        this.x = x;
        this.y = y;
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


    public Event getEvent(){ return event; }
    public void setEvent(Event e){ this.event = e; }
    public double[] getXY(){ return new double[]{x, y}; }
    public void setXY(int[] newXY){
        this.x = newXY[0];
        this.y = newXY[1];
    }

}
