package sysc3303.a1.group3;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DroneResponseTimeCalculator {

    private static final SimpleDateFormat HHMMSS = new SimpleDateFormat("HH:mm:ss");

    static class EventInfo {
        String drone;
        String eventDescription;
        String zone;
        Date eventTime;
        double assignedTime = -1;

        Double arrivalTime = null;
        Double extinguishStartTime = null;
        Double extinguishEndTime = null;
        String extinguishResult = null;
    }

    public static void run(String filename) {
        List<EventInfo> events = new ArrayList<>();
        Map<String, Date> eventCreationTimes = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Parse event trigger line
                if (line.startsWith("Event: ")) {
                    String eventLine = line.trim();
                    String timestampStr = eventLine.split(",")[0].split(" ")[1].trim();
                    Date eventTime = HHMMSS.parse(timestampStr);
                    eventCreationTimes.put(eventLine, eventTime);
                }


                if (line.contains("is scheduled with event: Event: ")) {
                    int tStart = line.indexOf('[');
                    int tEnd = line.indexOf(']');
                    double assignedTime = Double.parseDouble(line.substring(tStart + 1, tEnd).replace("s", ""));

                    int eventIndex = line.indexOf("Event: ");
                    String eventStr = line.substring(eventIndex + 7).trim(); // Strip "Event: "
                    String[] parts = line.split(" ");
                    String droneName = parts[1];
                    String zone = eventStr.split(",")[1].split(":")[1].trim();

                    EventInfo e = new EventInfo();
                    e.drone = droneName;
                    e.zone = zone;
                    e.assignedTime = assignedTime;
                    e.eventDescription = eventStr;
                    e.eventTime = eventCreationTimes.getOrDefault("Event: " + eventStr, null);
                    events.add(e);
                }


                if (line.matches("\\[.*s] Drone .* has arrived at Zone .*")) {
                    int t1 = line.indexOf('['), t2 = line.indexOf(']');
                    double time = Double.parseDouble(line.substring(t1 + 1, t2).replace("s", ""));
                    String[] parts = line.split(" ");
                    String drone = parts[2];
                    String zone = parts[7].replace(".", "");

                    for (int i = events.size() - 1; i >= 0; i--) {
                        EventInfo e = events.get(i);
                        if (e.drone.equals(drone) && e.zone.equals(zone) && e.arrivalTime == null) {
                            e.arrivalTime = time;
                            break;
                        }
                    }
                }

                // Parse extinguish start
                if (line.contains("has started extinguishing flames")) {
                    String[] parts = line.split(" ");
                    String drone = parts[1];
                    for (int i = events.size() - 1; i >= 0; i--) {
                        EventInfo e = events.get(i);
                        if (e.drone.equals(drone) && e.extinguishStartTime == null && e.arrivalTime != null) {
                            e.extinguishStartTime = e.arrivalTime; // Assume it starts on arrival
                            break;
                        }
                    }
                }

                // Parse foam run-out
                if (line.contains("has run out of foam")) {
                    int t1 = line.indexOf('['), t2 = line.indexOf(']');
                    double time = Double.parseDouble(line.substring(t1 + 1, t2).replace("s", ""));
                    String[] parts = line.split(" ");
                    String drone = parts[2]; // e.g., "Drone drone1 has..."

                    for (int i = events.size() - 1; i >= 0; i--) {
                        EventInfo e = events.get(i);
                        if (e.drone.equals(drone) && e.extinguishEndTime == null && e.arrivalTime != null) {
                            e.extinguishEndTime = time;
                            if (e.extinguishStartTime == null) {
                                e.extinguishStartTime = e.arrivalTime;
                            }
                            e.extinguishResult = "RanOut";
                            break;
                        }
                    }
                }

                // Parse successful extinguish
                if (line.contains("has finished extinguishing the flames")) {
                    int t1 = line.indexOf('['), t2 = line.indexOf(']');
                    double time = Double.parseDouble(line.substring(t1 + 1, t2).replace("s", ""));
                    String[] parts = line.split(" ");
                    String drone = parts[2];
                    for (int i = events.size() - 1; i >= 0; i--) {
                        EventInfo e = events.get(i);
                        if (e.drone.equals(drone) && e.extinguishEndTime == null) {
                            e.extinguishEndTime = time;
                            e.extinguishResult = "Completed";
                            break;
                        }
                    }
                }
            }


            System.out.println("\n--- Drone Event Response Summary ---");

            double totalResponseTime = 0;
            double totalArrivalTime = 0;
            double totalExtinguishTime = 0;
            int responseCount = 0, arrivalCount = 0, extinguishCount = 0;

            for (EventInfo e : events) {
                System.out.printf("Drone: %s | Zone: %s\n", e.drone, e.zone);

                if (e.eventTime != null && e.assignedTime >= 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(e.eventTime);
                    double eventSeconds = cal.get(Calendar.HOUR_OF_DAY) * 3600 +
                        cal.get(Calendar.MINUTE) * 60 +
                        cal.get(Calendar.SECOND);

                    double responseTime = e.assignedTime - eventSeconds;
                    System.out.printf("→ Response Time: %.3f seconds\n", responseTime);
                    totalResponseTime += responseTime;
                    responseCount++;
                }

                if (e.arrivalTime != null && e.assignedTime >= 0) {
                    double arrivalDelay = e.arrivalTime - e.assignedTime;
                    System.out.printf("→ Arrival Time After Assignment: %.3f seconds\n", arrivalDelay);
                    totalArrivalTime += arrivalDelay;
                    arrivalCount++;
                }

                if (e.extinguishStartTime != null && e.extinguishEndTime != null) {
                    double extinguishTime = e.extinguishEndTime - e.extinguishStartTime;
                    System.out.printf("→ Extinguish Duration: %.3f seconds (%s)\n", extinguishTime, e.extinguishResult);
                    totalExtinguishTime += extinguishTime;
                    extinguishCount++;
                } else {
                    System.out.println("→ Extinguish Info: Not recorded.");
                }

                System.out.println();
            }


            System.out.println("=== Drone Performance Summary (Averaged Across All Drones) ===");

            if (responseCount > 0)
                System.out.printf("Avg Response Time: %.3f seconds\n", totalResponseTime / responseCount);
            else
                System.out.println("Avg Response Time: N/A");

            if (arrivalCount > 0)
                System.out.printf("Avg Arrival Time After Assignment: %.3f seconds\n", totalArrivalTime / arrivalCount);
            else
                System.out.println("Avg Arrival Time After Assignment: N/A");

            if (extinguishCount > 0)
                System.out.printf("Avg Extinguish Time: %.3f seconds\n", totalExtinguishTime / extinguishCount);
            else
                System.out.println("Avg Extinguish Time: N/A");

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
