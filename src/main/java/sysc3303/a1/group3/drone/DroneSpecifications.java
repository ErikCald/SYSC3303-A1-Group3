package sysc3303.a1.group3.drone;

/**
 * Represents the physical specs of a Drone
 *
 * @param maxSpeed the max speed of a Drone, in m/s
 * @param maxAcceleration the max acceleration of a Drone, in m/s^2
 */
public record DroneSpecifications(double maxSpeed, double maxAcceleration) {

}
