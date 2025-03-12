package sysc3303.a1.group3.drone;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DroneRecord {

    private String droneName;
    private String state;
    private int port;
    private InetAddress address;

    public DroneRecord(String n, String s, int p, InetAddress a){
        this.droneName = n;
        this.state = s;
        this.port = p;
        this.address = a;
    }

    public String getDroneName(){ return droneName; }
    public String getState(){ return state; }
    public void setState(String s){ this.state = s; }
    public int getPort(){ return port; }
    public InetAddress getAddress(){ return address; }

}
