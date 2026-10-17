package com.udea.airline.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SeatInput(
        @NotBlank String id,
        @NotNull SeatType type,
        @NotNull Boolean occupied
) {
}
