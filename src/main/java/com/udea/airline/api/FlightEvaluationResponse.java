package com.udea.airline.api;

import java.util.List;

public record FlightEvaluationResponse(
        String flightId,
        List<PassengerResult> passengers,
        List<SeatResult> seats
) {
}
