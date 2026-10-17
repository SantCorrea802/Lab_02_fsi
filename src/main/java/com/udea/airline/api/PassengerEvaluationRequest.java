package com.udea.airline.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PassengerEvaluationRequest(
        @NotNull @Valid FlightInput flight,
        @NotNull @Valid PassengerInput passenger,
        @NotNull List<@NotNull @Valid SeatInput> seats
) {
}
