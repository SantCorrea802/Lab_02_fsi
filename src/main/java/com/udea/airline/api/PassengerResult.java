package com.udea.airline.api;

import java.math.BigDecimal;
import java.util.List;

public record PassengerResult(
        String passengerId,
        boolean upgradedToBusinessClass,
        boolean upgradeEligible,
        boolean priorityCheckIn,
        int discountPercent,
        BigDecimal compensationAmount,
        int loyaltyPointsAwarded,
        long totalLoyaltyPoints,
        boolean luggageAllowed,
        boolean vipLoungeAccess,
        String assignedSeatId,
        String assignedSeatType,
        String seatMessage,
        List<String> activatedRules
) {
}
