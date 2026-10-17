package com.udea.airline.service;

import com.udea.airline.api.FlightEvaluationRequest;
import com.udea.airline.api.FlightEvaluationResponse;
import com.udea.airline.api.PassengerInput;
import com.udea.airline.api.PassengerResult;
import com.udea.airline.api.SeatInput;
import com.udea.airline.rules.FlightFact;
import com.udea.airline.rules.PassengerDecision;
import com.udea.airline.rules.PassengerFact;
import com.udea.airline.rules.SeatInventory;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FlightEvaluationService {
    private final KieContainer kieContainer;

    public FlightEvaluationService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public FlightEvaluationResponse evaluate(FlightEvaluationRequest request) {
        verifyUniqueIds(request);
        SeatInventory inventory = new SeatInventory(request.seats());
        List<PassengerFact> passengers = new ArrayList<>();
        List<PassengerDecision> decisions = new ArrayList<>();
        for (int index = 0; index < request.passengers().size(); index++) {
            PassengerInput input = request.passengers().get(index);
            passengers.add(new PassengerFact(input.id(), index, input.age(), input.membership(),
                    input.luggageKg(), input.seatPreference(), input.travelingWithChildren(),
                    input.loyaltyPoints() == null ? 0 : input.loyaltyPoints()));
            decisions.add(new PassengerDecision(input.id()));
        }

        // Ningún hecho ni sesión se comparte entre peticiones. Las reglas escriben
        // beneficios únicos en cada decisión y el inventario solo vive en esta llamada.
        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(new FlightFact(request.flight().id(), request.flight().delayMinutes(),
                    request.flight().durationMinutes()));
            session.insert(inventory);
            for (int index = 0; index < passengers.size(); index++) {
                session.insert(passengers.get(index));
                session.insert(decisions.get(index));
            }
            session.fireAllRules();
        } finally {
            session.dispose();
        }

        List<PassengerResult> results = new ArrayList<>();
        for (int index = 0; index < passengers.size(); index++) {
            PassengerInput input = request.passengers().get(index);
            PassengerDecision decision = decisions.get(index);
            int originalPoints = input.loyaltyPoints() == null ? 0 : input.loyaltyPoints();
            results.add(new PassengerResult(input.id(), decision.isUpgradedToBusinessClass(),
                    decision.isUpgradeEligible(), decision.isPriorityCheckIn(),
                    decision.getDiscountPercent(), decision.getCompensationAmount(),
                    decision.getLoyaltyPointsAwarded(), (long) originalPoints + decision.getLoyaltyPointsAwarded(),
                    decision.isLuggageAllowed(), decision.isVipLoungeAccess(),
                    decision.getAssignedSeatId(), decision.getAssignedSeatType(),
                    seatMessage(input, decision), decision.getActivatedRules()));
        }
        return new FlightEvaluationResponse(request.flight().id(), results, inventory.results());
    }

    private void verifyUniqueIds(FlightEvaluationRequest request) {
        Set<String> passengerIds = new HashSet<>();
        for (PassengerInput passenger : request.passengers()) {
            if (!passengerIds.add(passenger.id())) {
                throw new IllegalArgumentException("El identificador de pasajero está duplicado: " + passenger.id());
            }
        }
        Set<String> seatIds = new HashSet<>();
        for (SeatInput seat : request.seats()) {
            if (!seatIds.add(seat.id())) {
                throw new IllegalArgumentException("El identificador de asiento está duplicado: " + seat.id());
            }
        }
    }

    private String seatMessage(PassengerInput input, PassengerDecision decision) {
        if (decision.getAssignedSeatId() != null) {
            return "Asiento " + decision.getAssignedSeatId() + " asignado.";
        }
        if (decision.isFamilySeatRequested() && input.age() >= 18 && input.age() <= 40) {
            return "No hay asiento familiar ni de salida de emergencia disponible.";
        }
        if (decision.isFamilySeatRequested()) {
            return "No hay asiento familiar disponible.";
        }
        if ("Any".equals(input.seatPreference()) && input.age() >= 18 && input.age() <= 40) {
            return "No hay asiento de salida de emergencia disponible.";
        }
        return "No aplica una regla de asignación de asiento.";
    }
}
