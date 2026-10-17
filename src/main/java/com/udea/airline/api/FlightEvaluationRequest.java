package com.udea.airline.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FlightEvaluationRequest(
        @NotNull @Valid FlightInput flight,
        @NotEmpty @Size(max = 1000) List<@NotNull @Valid PassengerInput> passengers,
        @NotNull List<@NotNull @Valid SeatInput> seats
) {
}
