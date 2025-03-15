package sysc3303.a1.group3.physics;

import java.util.Objects;
import java.util.StringJoiner;

public class Vector2d {

    public static final Vector2d ZERO = Vector2d.of(0, 0);

    private final double x;
    private final double y;

    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double magnitudeSquared() {
        return x * x + y * y;
    }

    public double magnitude() {
        return Math.sqrt(magnitudeSquared());
    }

    public Vector2d add(Vector2d v) {
        return of(this.x + v.x, this.y + v.y);
    }

    public Vector2d subtract(Vector2d vector) {
        return of(this.x - vector.x, this.y - vector.y);
    }

    public Vector2d multiply(double a) {
        return of(this.x * a, this.y * a);
    }

    public double dotProduct(Vector2d vector) {
        return this.x * vector.x + this.y * vector.y;
    }

    public Vector2d projectOnto(Vector2d vector) {
        double a = this.dotProduct(vector) / vector.magnitudeSquared();
        return Vector2d.of(a * x, a * y);
    }

    public Vector2d withMagnitude(double newMagnitude) {
        if (newMagnitude < 0) {
            throw new IllegalArgumentException("newMagnitude must be nonzero");
        }
        if (newMagnitude == 0) {
            return Vector2d.ZERO;
        }
        double magnitude = magnitude();
        if (magnitude == 0) {
            throw new IllegalStateException("Cannot change magnitude of a vector of zero magnitude");
        }

        double factor = newMagnitude / magnitude;
        return of(x * factor, y * factor);
    }

    public static Vector2d of(double x, double y) {
        if (Double.isNaN(x)) {
            throw new IllegalArgumentException("x is NaN");
        }
        if (Double.isNaN(y)) {
            throw new IllegalArgumentException("y is NaN");
        }
        return new Vector2d(x, y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector2d vector2d = (Vector2d) o;
        return Double.compare(x, vector2d.x) == 0 && Double.compare(y, vector2d.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Vector2d.class.getSimpleName() + "[", "]")
            .add("x=" + x)
            .add("y=" + y)
            .toString();
    }
}
