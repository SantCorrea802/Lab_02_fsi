package com.udea.airline.rules;

public final class FlightFact {
    private final String id;
    private final int delayMinutes;
    private final int durationMinutes;

    public FlightFact(String id, int delayMinutes, int durationMinutes) {
        this.id = id;
        this.delayMinutes = delayMinutes;
        this.durationMinutes = durationMinutes;
    }

    public String getId() { return id; }
    public int getDelayMinutes() { return delayMinutes; }
    public int getDurationMinutes() { return durationMinutes; }
}
