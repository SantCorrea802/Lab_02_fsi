package com.udea.airline.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PassengerInput(
        @NotBlank String id,
        @NotNull @Min(0) Integer age,
        @NotBlank String membership,
        @NotNull @DecimalMin("0.0") BigDecimal luggageKg,
        @NotBlank String seatPreference,
        @NotNull Boolean travelingWithChildren,
        @Min(0) Integer loyaltyPoints
) {
}
