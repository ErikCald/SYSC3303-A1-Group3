package sysc3303.a1.group3.physics;

import sysc3303.a1.group3.drone.Drone;

public class Kinematics {
    private static final int TICK_QTY = Drone.DRONE_TRAVEL_SPEEDUP;
    private static final double SECONDS_PER_TICK = Drone.DRONE_LOOP_SLEEP_MS / 1000.0;

    static final double EPSILON = Math.ulp(1.0d);
    private final double ALLOWABLE_ERROR = 50; // zones are general 100x100 so 50 is a good error from center

    private final double maxSpeed;
    private final double maxSpeedSquared;
    private final double maxAcceleration;

    private Vector2d position = Vector2d.ZERO;
    private Vector2d velocity = Vector2d.ZERO;

    private Vector2d lastAcceleration = Vector2d.ZERO;

    private Vector2d target = null;

    public Kinematics(double maxSpeed, double maxAcceleration) {
        this.maxSpeed = Math.abs(maxSpeed);
        this.maxSpeedSquared = this.maxSpeed * this.maxSpeed;
        this.maxAcceleration = Math.abs(maxAcceleration);
    }

    public Kinematics(double maxSpeed, double maxAcceleration, Vector2d position) {
        this.maxSpeed = Math.abs(maxSpeed);
        this.maxSpeedSquared = this.maxSpeed * this.maxSpeed;
        this.maxAcceleration = Math.abs(maxAcceleration);
        this.position = position;
    }

    public Vector2d getPosition() {
        return position;
    }

    public void setPosition(Vector2d p) { position = p;}

    public Vector2d getVelocity() {
        return velocity;
    }

    public Vector2d getLastAcceleration() {
        return lastAcceleration;
    }

    public Vector2d getTarget() {
        return target;
    }

    public void setTarget(Vector2d target) {
        this.target = target;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public boolean isAtTarget() {
        return (target != null) 
            && (Math.abs(position.getX() - target.getX()) < ALLOWABLE_ERROR) 
            && (Math.abs(position.getY() - target.getY()) < ALLOWABLE_ERROR);
    }

    public void tick() {
        for (int i = 0; i < TICK_QTY; i++) {
            // Weird "algorithm" that works alright at lower tick amounts
            Vector2d targetDirection = target.subtract(position);
            Vector2d accelerationDirection = targetDirection.subtract(velocity); // the main choice
            Vector2d acceleration = accelerationDirection.withMagnitude(maxAcceleration);

            updateMotion(SECONDS_PER_TICK, acceleration);

            if(isAtTarget()) {
                break;
            }
        }
    }

    private void updateMotion(double duration, Vector2d acceleration) {

        // Velocity if we accelerate this whole time
        double theoreticalSpeedSquared = velocityAfter(velocity, acceleration, duration).magnitudeSquared();

        if (theoreticalSpeedSquared >= maxSpeedSquared) {
            // We would end up going too fast. Need to limit acceleration for only a period of time.

            double accelerationDuration = timeToReachSpeed(velocity, acceleration, maxSpeed);
            double remainingDuration = duration - accelerationDuration;

            // Update motion for this accelerating period
            accelerate(accelerationDuration, acceleration);

            // Update motion for period after where the velocity is constant
            // d_f = d_i + v * t
            position = position.add(velocity.multiply(remainingDuration));
            lastAcceleration = Vector2d.ZERO;

        } else {
            // Can accelerate the whole time
            accelerate(duration, acceleration);
        }
    }

    private void accelerate(double duration, Vector2d acceleration) {
        if (duration == 0) {
            return;
        }

        // Update position
        // d_f = d_i + (v_i * t) + (0.5 * a * t^2)
        Vector2d velocityTerm = velocity.multiply(duration);
        Vector2d accelerationTerm = acceleration.multiply(0.5 * duration * duration);
        position = position.add(velocityTerm).add(accelerationTerm);

        // Do this after since the previous calculating requires the *initial* velocity.
        velocity = velocityAfter(velocity, acceleration, duration);

        lastAcceleration = acceleration;
    }

    static Vector2d velocityAfter(Vector2d initial, Vector2d acceleration, double duration) {
        // v_f = v_i + a * t
        return initial.add(acceleration.multiply(duration));
    }

    static double timeToReachSpeed(Vector2d velocity, Vector2d acceleration, double speed) {
        // Sacrifice the square operation here for a safer check (epsilon)
        if (equalsNearly(velocity.magnitude(), speed)) {
            return 0;
        }
        if (acceleration.magnitudeSquared() == 0) {
            throw new IllegalArgumentException("Acceleration is zero");
        }

        double vx = velocity.getX();
        double vy = velocity.getY();
        double ax = acceleration.getX();
        double ay = acceleration.getY();

        // Magnitude (squared) of the resultant velocity vector:
        // speed^2 = (vx + ax * t)^2 + (vy + ay * t)^2
        // This quadratic equation is rearranged below to find the roots (time).

        double a = (ax * ax) + (ay * ay);
        double b = 2 * ((vx * ax) + (vy * ay));
        double c = (vx * vx) + (vy * vy) - (speed * speed);

        double time = smallestNonNegativeRoot(a, b, c);
        if (Double.isNaN(time)) {
            // System.out.println(System.currentTimeMillis() + ": timeToReachSpeed failed to early-check velocity already being at the max.");
            return 0;
        }
        return time;
    }

    private static double smallestNonNegativeRoot(double a, double b, double c) {
        double root = Math.sqrt(b * b - 4 * a * c);
        if (Double.isNaN(root)) {
            return Double.NaN;
        }

        double first = (-b - root) / (2 * a);
        double second = (-b + root) / (2 * a);

        if (first >= 0) {
            return first;
        }
        if (second >= 0) {
            return second;
        }
        return Double.NaN;
    }

    private static boolean equalsNearly(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }
}
