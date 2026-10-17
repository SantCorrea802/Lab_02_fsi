package com.udea.airline.rules;

import java.math.BigDecimal;

public final class PassengerFact {
    private final String id;
    private final int order;
    private final int age;
    private final String membership;
    private final BigDecimal luggageKg;
    private final String seatPreference;
    private final boolean travelingWithChildren;
    private final int loyaltyPoints;

    public PassengerFact(String id, int order, int age, String membership, BigDecimal luggageKg,
                         String seatPreference, boolean travelingWithChildren, int loyaltyPoints) {
        this.id = id;
        this.order = order;
        this.age = age;
        this.membership = membership;
        this.luggageKg = luggageKg;
        this.seatPreference = seatPreference;
        this.travelingWithChildren = travelingWithChildren;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String getId() { return id; }
    public int getOrder() { return order; }
    public int getAge() { return age; }
    public String getMembership() { return membership; }
    public BigDecimal getLuggageKg() { return luggageKg; }
    public String getSeatPreference() { return seatPreference; }
    public boolean isTravelingWithChildren() { return travelingWithChildren; }
    public int getLoyaltyPoints() { return loyaltyPoints; }
}
