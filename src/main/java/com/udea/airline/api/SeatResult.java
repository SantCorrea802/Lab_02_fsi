package com.udea.airline.api;

public record SeatResult(String id, SeatType type, boolean occupied, String assignedPassengerId) {
}
