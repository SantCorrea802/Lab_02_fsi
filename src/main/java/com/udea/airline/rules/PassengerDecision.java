package com.udea.airline.rules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PassengerDecision {
    private final String passengerId;
    private boolean upgradeRequested;
    private boolean upgradeEligible = true;
    private boolean priorityCheckIn;
    private int discountPercent;
    private BigDecimal compensationAmount = BigDecimal.ZERO;
    private int loyaltyPointsAwarded;
    private boolean luggageAllowed = true;
    private boolean vipLoungeAccess;
    private boolean familySeatRequested;
    private boolean emergencySeatRequested;
    private String assignedSeatId;
    private String assignedSeatType;
    private final Set<String> activatedRules = new HashSet<>();

    public PassengerDecision(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getPassengerId() { return passengerId; }
    public boolean isUpgradedToBusinessClass() { return upgradeRequested && upgradeEligible; }
    public boolean isUpgradeEligible() { return upgradeEligible; }
    public boolean isPriorityCheckIn() { return priorityCheckIn; }
    public int getDiscountPercent() { return discountPercent; }
    public BigDecimal getCompensationAmount() { return compensationAmount; }
    public int getLoyaltyPointsAwarded() { return loyaltyPointsAwarded; }
    public boolean isLuggageAllowed() { return luggageAllowed; }
    public boolean isVipLoungeAccess() { return vipLoungeAccess; }
    public boolean isFamilySeatRequested() { return familySeatRequested; }
    public boolean isEmergencySeatRequested() { return emergencySeatRequested; }
    public String getAssignedSeatId() { return assignedSeatId; }
    public String getAssignedSeatType() { return assignedSeatType; }

    public void requestBusinessUpgrade() { upgradeRequested = true; }
    public void denyBusinessUpgrade() { upgradeEligible = false; }
    public void setPriorityCheckIn(boolean value) { priorityCheckIn = value; }
    public void setDiscountPercent(int value) { discountPercent = value; }
    public void setCompensationAmount(BigDecimal value) { compensationAmount = value; }
    public void setLoyaltyPointsAwarded(int value) { loyaltyPointsAwarded = value; }
    public void setLuggageAllowed(boolean value) { luggageAllowed = value; }
    public void setVipLoungeAccess(boolean value) { vipLoungeAccess = value; }
    public void setFamilySeatRequested(boolean value) { familySeatRequested = value; }
    public void setEmergencySeatRequested(boolean value) { emergencySeatRequested = value; }

    public void assignSeat(String seatId, String seatType) {
        if (seatId != null) {
            assignedSeatId = seatId;
            assignedSeatType = seatType;
        }
    }

    public void recordRule(String ruleName) { activatedRules.add(ruleName); }

    public List<String> getActivatedRules() {
        List<String> result = new ArrayList<>(activatedRules);
        Collections.sort(result);
        return result;
    }
}
