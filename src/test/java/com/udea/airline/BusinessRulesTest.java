package com.udea.airline;

import com.udea.airline.api.FlightEvaluationRequest;
import com.udea.airline.api.FlightEvaluationResponse;
import com.udea.airline.api.FlightInput;
import com.udea.airline.api.PassengerInput;
import com.udea.airline.api.PassengerResult;
import com.udea.airline.api.SeatInput;
import com.udea.airline.api.SeatType;
import com.udea.airline.rules.FlightFact;
import com.udea.airline.rules.PassengerDecision;
import com.udea.airline.rules.PassengerFact;
import com.udea.airline.rules.SeatInventory;
import com.udea.airline.service.FlightEvaluationService;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BusinessRulesTest {
    @Autowired FlightEvaluationService service;
    @Autowired KieContainer kieContainer;

    private PassengerInput passenger(String id, int age, String membership, String luggage,
                                     String preference, boolean children, int points) {
        return new PassengerInput(id, age, membership, new BigDecimal(luggage), preference, children, points);
    }

    private FlightEvaluationResponse evaluate(int delay, int duration, List<PassengerInput> passengers,
                                              List<SeatInput> seats) {
        return service.evaluate(new FlightEvaluationRequest(new FlightInput("AV-101", delay, duration),
                passengers, seats));
    }

    private PassengerResult one(int delay, int duration, PassengerInput passenger, List<SeatInput> seats) {
        return evaluate(delay, duration, List.of(passenger), seats).passengers().get(0);
    }

    private SeatInput free(String id, SeatType type) { return new SeatInput(id, type, false); }

    @Test
    void exactlyTenDroolsRulesAreLoaded() {
        long count = kieContainer.getKieBase().getKiePackages().stream()
                .mapToLong(pkg -> pkg.getRules().size()).sum();
        assertEquals(10, count);
    }

    @Test
    void upgradeNeedsGoldOrPlatinumAndDelayStrictlyAboveSixty() {
        for (String membership : List.of("Gold", "Platinum")) {
            PassengerInput p = passenger("P", 30, membership, "12", "Window", false, 0);
            assertFalse(one(60, 180, p, List.of()).upgradedToBusinessClass());
            PassengerResult result = one(61, 180, p, List.of());
            assertTrue(result.upgradedToBusinessClass());
            assertTrue(result.activatedRules().contains("UpgradeToBusinessClassForFrequentFlyersWithDelays"));
        }
        assertFalse(one(61, 180, passenger("P", 30, "Basic", "12", "Window", false, 0),
                List.of()).upgradedToBusinessClass());
    }

    @Test
    void priorityCheckInStartsAfterAgeSixtyFive() {
        assertFalse(one(0, 180, passenger("P", 65, "Basic", "12", "Window", false, 0),
                List.of()).priorityCheckIn());
        assertTrue(one(0, 180, passenger("P", 66, "Basic", "12", "Window", false, 0),
                List.of()).priorityCheckIn());
    }

    @Test
    void discountRequiresLessThanTenKilograms() {
        assertEquals(10, one(0, 180, passenger("P", 30, "Basic", "9.99", "Window", false, 0),
                List.of()).discountPercent());
        assertEquals(0, one(0, 180, passenger("P", 30, "Basic", "10", "Window", false, 0),
                List.of()).discountPercent());
    }

    @Test
    void overweightDenyStartsStrictlyAboveTwentyThreeKilograms() {
        assertTrue(one(0, 180, passenger("P", 30, "Basic", "23", "Window", false, 0),
                List.of()).upgradeEligible());
        assertFalse(one(0, 180, passenger("P", 30, "Basic", "23.01", "Window", false, 0),
                List.of()).upgradeEligible());
    }

    @Test
    void emergencySeatRequiresInclusiveAgeRangeAnyAndAvailability() {
        for (int age : List.of(18, 40)) {
            PassengerResult result = one(0, 180,
                    passenger("P", age, "Basic", "12", "Any", false, 0),
                    List.of(free("E1", SeatType.EMERGENCY_EXIT)));
            assertEquals("E1", result.assignedSeatId());
            assertEquals("EMERGENCY_EXIT", result.assignedSeatType());
        }
        for (int age : List.of(17, 41)) {
            assertNull(one(0, 180, passenger("P", age, "Basic", "12", "Any", false, 0),
                    List.of(free("E1", SeatType.EMERGENCY_EXIT))).assignedSeatId());
        }
        assertNull(one(0, 180, passenger("P", 30, "Basic", "12", "Window", false, 0),
                List.of(free("E1", SeatType.EMERGENCY_EXIT))).assignedSeatId());
        PassengerResult unavailable = one(0, 180, passenger("P", 30, "Basic", "12", "Any", false, 0),
                List.of(new SeatInput("E1", SeatType.EMERGENCY_EXIT, true)));
        assertNull(unavailable.assignedSeatId());
        assertTrue(unavailable.seatMessage().contains("No hay asiento de salida de emergencia"));
    }

    @Test
    void compensationRequiresDelayStrictlyAboveThreeHours() {
        PassengerInput p = passenger("P", 30, "Basic", "12", "Window", false, 0);
        assertEquals(0, BigDecimal.ZERO.compareTo(one(180, 180, p, List.of()).compensationAmount()));
        assertEquals(0, new BigDecimal("200").compareTo(one(181, 180, p, List.of()).compensationAmount()));
    }

    @Test
    void longFlightPointsRequireNonBasicMembershipAndMoreThanFiveHours() {
        PassengerInput gold = passenger("P", 30, "Gold", "12", "Window", false, 120);
        assertEquals(0, one(0, 300, gold, List.of()).loyaltyPointsAwarded());
        PassengerResult awarded = one(0, 301, gold, List.of());
        assertEquals(500, awarded.loyaltyPointsAwarded());
        assertEquals(620, awarded.totalLoyaltyPoints());
        assertEquals(0, one(0, 301,
                passenger("P", 30, "Basic", "12", "Window", false, 120),
                List.of()).loyaltyPointsAwarded());
    }

    @Test
    void luggageRestrictionUsesStrictWeightAndDurationLimits() {
        assertFalse(one(0, 119, passenger("P", 30, "Basic", "15.01", "Window", false, 0),
                List.of()).luggageAllowed());
        assertTrue(one(0, 119, passenger("P", 30, "Basic", "15", "Window", false, 0),
                List.of()).luggageAllowed());
        assertTrue(one(0, 120, passenger("P", 30, "Basic", "15.01", "Window", false, 0),
                List.of()).luggageAllowed());
    }

    @Test
    void vipAccessRequiresPlatinum() {
        assertTrue(one(0, 180, passenger("P", 30, "Platinum", "12", "Window", false, 0),
                List.of()).vipLoungeAccess());
        assertFalse(one(0, 180, passenger("P", 30, "Gold", "12", "Window", false, 0),
                List.of()).vipLoungeAccess());
    }

    @Test
    void familySeatRequiresChildrenAndNoSpecificPreference() {
        List<SeatInput> seats = List.of(free("F1", SeatType.FAMILY));
        assertEquals("F1", one(0, 180,
                passenger("P", 45, "Basic", "12", "Any", true, 0), seats).assignedSeatId());
        assertNull(one(0, 180,
                passenger("P", 45, "Basic", "12", "Any", false, 0), seats).assignedSeatId());
        assertNull(one(0, 180,
                passenger("P", 45, "Basic", "12", "Aisle", true, 0), seats).assignedSeatId());
        PassengerResult noSeat = one(0, 180,
                passenger("P", 45, "Basic", "12", "Any", true, 0), List.of());
        assertNull(noSeat.assignedSeatId());
        assertTrue(noSeat.seatMessage().contains("No hay asiento familiar"));
    }

    @Test
    void overweightVetoWinsWithoutLosingOtherBenefits() {
        PassengerResult result = one(181, 301,
                passenger("P", 66, "Platinum", "24", "Window", false, 50), List.of());
        assertFalse(result.upgradeEligible());
        assertFalse(result.upgradedToBusinessClass());
        assertTrue(result.priorityCheckIn());
        assertEquals(0, new BigDecimal("200").compareTo(result.compensationAmount()));
        assertEquals(500, result.loyaltyPointsAwarded());
        assertTrue(result.vipLoungeAccess());
        assertTrue(result.activatedRules().contains("UpgradeToBusinessClassForFrequentFlyersWithDelays"));
        assertTrue(result.activatedRules().contains("DenyUpgradeForOverweightLuggage"));
    }

    @Test
    void familySeatHasPriorityAndNoSeatIsAssignedTwice() {
        PassengerInput family = passenger("F", 30, "Basic", "12", "Any", true, 0);
        PassengerInput solo = passenger("S", 30, "Basic", "12", "Any", false, 0);
        FlightEvaluationResponse response = evaluate(0, 180, List.of(family, solo),
                List.of(free("F1", SeatType.FAMILY), free("E1", SeatType.EMERGENCY_EXIT)));
        assertEquals("F1", response.passengers().get(0).assignedSeatId());
        assertEquals("E1", response.passengers().get(1).assignedSeatId());
        assertFalse(response.passengers().get(0).activatedRules()
                .contains("AssignEmergencyExitSeatToYoungAdults"));
        assertEquals(2, response.seats().stream().filter(seat -> seat.occupied()).count());
        assertEquals(2, response.seats().stream().map(seat -> seat.assignedPassengerId()).distinct().count());
    }

    @Test
    void oneEmergencySeatIsAssignedOnlyOnceInRequestOrder() {
        PassengerInput first = passenger("A", 30, "Basic", "12", "Any", false, 0);
        PassengerInput second = passenger("B", 30, "Basic", "12", "Any", false, 0);
        FlightEvaluationResponse response = evaluate(0, 180, List.of(first, second),
                List.of(free("E1", SeatType.EMERGENCY_EXIT)));
        assertEquals("E1", response.passengers().get(0).assignedSeatId());
        assertNull(response.passengers().get(1).assignedSeatId());
        assertFalse(response.passengers().get(1).activatedRules()
                .contains("AssignEmergencyExitSeatToYoungAdults"));
        assertEquals("A", response.seats().get(0).assignedPassengerId());
    }

    @Test
    void familyPassengerUsesEmergencySeatOnlyWhenFamilySeatIsUnavailable() {
        PassengerInput family = passenger("F", 30, "Basic", "12", "Any", true, 0);
        PassengerResult result = one(0, 180, family, List.of(free("E1", SeatType.EMERGENCY_EXIT)));
        assertEquals("E1", result.assignedSeatId());
        assertTrue(result.activatedRules().contains("PreferentialSeatForFamilies"));
        assertTrue(result.activatedRules().contains("AssignEmergencyExitSeatToYoungAdults"));
    }

    @Test
    void overweightOnShortDelayedFlightDeniesUpgradeAndLuggageSeparately() {
        PassengerResult result = one(61, 119,
                passenger("P", 30, "Gold", "24", "Window", false, 0), List.of());
        assertFalse(result.upgradedToBusinessClass());
        assertFalse(result.upgradeEligible());
        assertFalse(result.luggageAllowed());
        assertTrue(result.activatedRules().contains("UpgradeToBusinessClassForFrequentFlyersWithDelays"));
        assertTrue(result.activatedRules().contains("DenyUpgradeForOverweightLuggage"));
        assertTrue(result.activatedRules().contains("RestrictLuggageOnShortFlights"));
    }

    @Test
    void twoIndependentEvaluationsReturnSameResultsWithoutAccumulation() {
        FlightEvaluationRequest request = new FlightEvaluationRequest(new FlightInput("AV-101", 181, 301),
                List.of(passenger("P", 30, "Platinum", "9", "Any", false, 25)),
                List.of(free("E1", SeatType.EMERGENCY_EXIT)));
        FlightEvaluationResponse first = service.evaluate(request);
        FlightEvaluationResponse second = service.evaluate(request);
        assertEquals(first, second);
        assertEquals(500, second.passengers().get(0).loyaltyPointsAwarded());
        assertEquals(525, second.passengers().get(0).totalLoyaltyPoints());
        assertEquals(0, new BigDecimal("200").compareTo(second.passengers().get(0).compensationAmount()));
        assertEquals("E1", second.passengers().get(0).assignedSeatId());
    }

    @Test
    void firingTheSameSessionAgainDoesNotDuplicateBenefits() {
        KieSession session = kieContainer.newKieSession();
        PassengerDecision decision = new PassengerDecision("P");
        try {
            session.insert(new FlightFact("AV-101", 181, 301));
            session.insert(new SeatInventory(List.of(free("E1", SeatType.EMERGENCY_EXIT))));
            session.insert(new PassengerFact("P", 0, 30, "Gold", new BigDecimal("9"), "Any", false, 10));
            session.insert(decision);
            assertTrue(session.fireAllRules() > 0);
            assertEquals(0, session.fireAllRules());
            assertEquals(500, decision.getLoyaltyPointsAwarded());
            assertEquals(0, new BigDecimal("200").compareTo(decision.getCompensationAmount()));
        } finally {
            session.dispose();
        }
    }
}
