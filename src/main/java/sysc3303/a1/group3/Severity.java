package sysc3303.a1.group3;

/**
 * Represents the severity of a fire incident.
 */
public enum Severity {
    Low(10),
    Moderate(20),
    High(30);

    // The required amount of foam to extinguish the fire.
    private final int requiredLitersOfWater;

    private Severity(int requiredLitersOfWater) {
        this.requiredLitersOfWater = requiredLitersOfWater;
    }

    public static Severity fromString(String severity) {
        switch (severity.toUpperCase()) {
            case "LOW":
                return Low;
            case "MODERATE":
                return Moderate;
            case "HIGH":
                return High;
            default:
                throw new IllegalArgumentException("Invalid severity: " + severity);
        }
    }

    public int getRequiredLetersOfFoam() {
        return requiredLitersOfWater;
    }
}
