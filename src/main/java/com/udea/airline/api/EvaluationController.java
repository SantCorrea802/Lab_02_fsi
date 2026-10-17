package com.udea.airline.api;

import com.udea.airline.service.FlightEvaluationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {
    private final FlightEvaluationService service;

    public EvaluationController(FlightEvaluationService service) {
        this.service = service;
    }

    @PostMapping("/passenger")
    public FlightEvaluationResponse evaluatePassenger(@Valid @RequestBody PassengerEvaluationRequest request) {
        return service.evaluate(new FlightEvaluationRequest(request.flight(),
                List.of(request.passenger()), request.seats()));
    }

    @PostMapping("/flight")
    public FlightEvaluationResponse evaluateFlight(@Valid @RequestBody FlightEvaluationRequest request) {
        return service.evaluate(request);
    }
}
