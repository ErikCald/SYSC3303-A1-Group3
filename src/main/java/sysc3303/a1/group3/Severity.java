package sysc3303.a1.group3;

/**
 * Represents the severity of a fire incident.
 * Stores the required amount of foam to extinguish the fire.
 */
public enum Severity {
    Low(10),
    Moderate(20),
    High(30);

    // The required amount of foam to extinguish the fire.
    private final int requiredLitersOfWater;

    /**
     * Create a new severity.
     * 
     * @param requiredLitersOfWater The required amount of foam to extinguish the fire.
     */
    private Severity(int requiredLitersOfWater) {
        this.requiredLitersOfWater = requiredLitersOfWater;
    }

    /**
     * Get the severity from a string.
     * 
     * @param severity The string representation of the severity.
     * @return The severity.
     */
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

    /**
     * Get the required amount of foam to extinguish the fire.
     * 
     * @return The required amount of foam to extinguish the fire.
     */
    public int getRequiredLetersOfFoam() {
        return requiredLitersOfWater;
    }
}
