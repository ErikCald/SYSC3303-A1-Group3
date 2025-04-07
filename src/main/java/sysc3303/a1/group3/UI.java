package sysc3303.a1.group3;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import sysc3303.a1.group3.drone.DroneRecord;
import sysc3303.a1.group3.physics.Vector2d;

public class UI {
    private final int borderSize = 20;
    private final int legendWidth = 190;
    private final int topBorder = 20;
    private final int droneStatusBoxSize = 20;
    private final int droneStatusSpacing = 40;
    private final int droneStatusBottomOffset = 30;

    private JFrame frame;
    private List<Zone> zones = new ArrayList<>();
    private List<DroneRecord> drones = new ArrayList<>();
    private List<Integer> fireStates = new ArrayList<>();
    private int minX, minY, maxX, maxY;

    public UI(List<Zone> zones) {
        this.zones = zones;
        calculateZoneBounds();
        frame = new JFrame("Zone and Drone Display");
        frame.setSize(maxX - minX + 400, maxY - minY + 2 * borderSize + topBorder + droneStatusBottomOffset + droneStatusBoxSize + 50);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBorder(g);
                drawZones(g);
                drawDrones(g);
                drawLegend(g);
                drawFireSpots(g);
                drawDroneStatuses(g);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
        frame.repaint();
    }

    private void calculateZoneBounds() {
        minX = Integer.MAX_VALUE;
        minY = Integer.MAX_VALUE;
        maxX = Integer.MIN_VALUE;
        maxY = Integer.MIN_VALUE;

        for (Zone zone : zones) {
            minX = Math.min(minX, zone.startX());
            minY = Math.min(minY, zone.startY());
            maxX = Math.max(maxX, zone.endX());
            maxY = Math.max(maxY, zone.endY());
        }
    }

    public void updateDrones(List<DroneRecord> drones) {
        this.drones = drones;
        frame.getContentPane().repaint();
    }

    public void updateFireStates(List<Integer> fireZoneIds) {
        this.fireStates = fireZoneIds;
        frame.getContentPane().repaint();
    }

    private void drawBorder(Graphics g) {
        g.setColor(Color.BLACK);
        g.drawRect(legendWidth - borderSize, topBorder, maxX - minX + 2 * borderSize, maxY - minY + borderSize + topBorder);
    }

    private void drawZones(Graphics g) {
        for (Zone zone : zones) {
            g.drawRect(zone.startX() - minX + legendWidth, zone.startY() - minY + borderSize + topBorder, zone.endX() - zone.startX(), zone.endY() - zone.startY());
            g.drawString("Zone " + zone.zoneID(), zone.startX() - minX + 5 + legendWidth, topBorder + zone.startY() - minY + 15 + borderSize);
        }
    }

    private void drawDrones(Graphics g) {
        int borderSize = 20;
        int legendWidth = 190;
        for (int i = 0; i < drones.size(); i++) {
            DroneRecord drone = drones.get(i);
            Vector2d dronePosition = drone.getPosition();
            int x = (int) dronePosition.getX() - 15 - minX + legendWidth;
            int y = (int) dronePosition.getY() - 15 - minY + borderSize + topBorder;
            g.setColor(getDroneColor(drone.getState()));
            g.fillRect(x, y, 30, 30);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, 30, 30);
            String droneName = drone.getDroneName();
            if (droneName.toLowerCase().contains("drone")) {
                int index = droneName.toLowerCase().indexOf("drone");
                String rest = droneName.substring(index + "drone".length()).trim();
                droneName = "D(" + rest + ")";
            }
            g.drawString(droneName, x, y + 20);
        }
    }

    private void drawFireSpots(Graphics g) {
        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            Vector2d center = zone.centre();
            int x = (int) center.getX() - 10 - minX + legendWidth;
            int y = (int) center.getY() - 10 - minY + borderSize + topBorder;

            if (fireStates.contains(zone.zoneID())) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.GREEN);
            }

            g.fillRect(x, y, 20, 20);
        }
    }

    private Color getDroneColor(String state) {
        switch (state) {
            case "DroneEnRoute":
                return Color.YELLOW;
            case "DroneInZone":
            case "DroneDroppingFoam":
                return Color.BLUE;
            case "DroneReturning":
                return new Color(128, 0, 128);
            case "DroneNozzleJam":
            case "DroneStuck":
                return Color.DARK_GRAY;
            case "DroneIdle":
                return Color.LIGHT_GRAY;
            default:
                System.out.println("Unknown drone state: " + state);
                return Color.GRAY;
        }
    }

    private void drawLegend(Graphics g) {
        int legendX = 10;
        int legendY = 20;

        g.setColor(Color.RED);
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Active Fire", legendX + 25, legendY + 15);

        legendY += 30;
        g.setColor(Color.GREEN);
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Extinguished Fire", legendX + 25, legendY + 15);

        legendY += 30;
        g.setColor(Color.YELLOW);
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Drone outbound", legendX + 25, legendY + 15);

        legendY += 30;
        g.setColor(Color.BLUE);
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Drone Extinguishing fire", legendX + 25, legendY + 15);

        legendY += 30;
        g.setColor(new Color(128, 0, 128));
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Drone Returning", legendX + 25, legendY + 15);


        legendY += 30;
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Drone Idle", legendX + 25, legendY + 15);

        legendY += 30;
        g.setColor(Color.DARK_GRAY);
        g.fillRect(legendX, legendY, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString("Drone Fault", legendX + 25, legendY + 15);
    }

    private void drawDroneStatuses(Graphics g) {
        int startX = borderSize + droneStatusSpacing;
        int startY = frame.getHeight() - droneStatusBottomOffset - droneStatusBoxSize - 40;

        for (int i = 0; i < drones.size(); i++) {
            DroneRecord drone = drones.get(i);
            String state = drone.getState();
            Color stateColor = getDroneColor(state);

            int x = startX + i * (droneStatusBoxSize + droneStatusSpacing);
            int y = startY + 20;

            // Draw drone name
            String droneName = drone.getDroneName();
            if (droneName.toLowerCase().contains("drone")) {
                int index = droneName.toLowerCase().indexOf("drone");
                String rest = droneName.substring(index + "drone".length()).trim();
                droneName = "D(" + rest + ")";
            }
            g.setColor(Color.BLACK);
            g.drawString(droneName, x, y - 5);

            // Draw state box
            g.setColor(stateColor);
            g.fillRect(x, y, droneStatusBoxSize, droneStatusBoxSize);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, droneStatusBoxSize, droneStatusBoxSize);
        }
    }

    public static void main(String[] args) {
        List<Zone> zones = new ArrayList<>();
        zones.add(Zone.of(1, 50, 50, 200, 200));
        zones.add(Zone.of(2, 300, 100, 450, 250));

        UI ui = new UI(zones);

        List<DroneRecord> drones = new ArrayList<>();
        drones.add(new DroneRecord("Drone A", "DroneEnRoute", 100, 100));
        drones.add(new DroneRecord("Drone B", "DroneInZone", 350, 150));
        drones.add(new DroneRecord("Drone Helicopter", "DroneEnRoute", 200, 200));

        List<Integer> fireZones = new ArrayList<>();
        fireZones.add(1);
        fireZones.add(3);

        ui.updateDrones(drones);
        ui.updateFireStates(fireZones);
    }
}