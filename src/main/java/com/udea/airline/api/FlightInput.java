package com.udea.airline.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FlightInput(
        @NotBlank String id,
        @NotNull @Min(0) Integer delayMinutes,
        @NotNull @Min(1) Integer durationMinutes
) {
}
