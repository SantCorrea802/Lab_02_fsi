package com.udea.airline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.udea.airline.api.FlightEvaluationRequest;
import com.udea.airline.api.FlightInput;
import com.udea.airline.api.PassengerInput;
import com.udea.airline.api.SeatInput;
import com.udea.airline.api.SeatType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EvaluationApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private PassengerInput passenger(String id) {
        return new PassengerInput(id, 30, "Gold", new BigDecimal("9"), "Any", false, 100);
    }

    @Test
    void passengerEndpointAppliesDroolsRulesThroughHttpLayer() throws Exception {
        String json = """
                {
                  "flight": {"id":"AV-101","delayMinutes":181,"durationMinutes":301},
                  "passenger": {"id":"P1","age":30,"membership":"Gold","luggageKg":9,
                                "seatPreference":"Any","travelingWithChildren":false,"loyaltyPoints":100},
                  "seats": [{"id":"E1","type":"EMERGENCY_EXIT","occupied":false}]
                }
                """;
        mvc.perform(post("/api/evaluations/passenger").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers[0].upgradedToBusinessClass").value(true))
                .andExpect(jsonPath("$.passengers[0].discountPercent").value(10))
                .andExpect(jsonPath("$.passengers[0].compensationAmount").value(200))
                .andExpect(jsonPath("$.passengers[0].totalLoyaltyPoints").value(600))
                .andExpect(jsonPath("$.passengers[0].assignedSeatId").value("E1"))
                .andExpect(jsonPath("$.seats[0].occupied").value(true))
                .andExpect(jsonPath("$.seats[0].assignedPassengerId").value("P1"));
    }

    @Test
    void flightEndpointAllocatesSeatOnlyOnce() throws Exception {
        FlightEvaluationRequest request = new FlightEvaluationRequest(new FlightInput("AV-101", 0, 180),
                List.of(passenger("P1"), passenger("P2")),
                List.of(new SeatInput("E1", SeatType.EMERGENCY_EXIT, false)));
        mvc.perform(post("/api/evaluations/flight").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passengers[0].assignedSeatId").value("E1"))
                .andExpect(jsonPath("$.passengers[1].assignedSeatId").isEmpty())
                .andExpect(jsonPath("$.seats[0].assignedPassengerId").value("P1"));
    }

    @Test
    void validationRejectsNegativeDelayAndMissingMembership() throws Exception {
        String json = """
                {
                  "flight": {"id":"AV-101","delayMinutes":-1,"durationMinutes":180},
                  "passenger": {"id":"P1","age":30,"membership":"","luggageKg":9,
                                "seatPreference":"Any","travelingWithChildren":false},
                  "seats": []
                }
                """;
        mvc.perform(post("/api/evaluations/passenger").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Datos de entrada inválidos"))
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void duplicatePassengerIdsAreRejected() throws Exception {
        FlightEvaluationRequest request = new FlightEvaluationRequest(new FlightInput("AV-101", 0, 180),
                List.of(passenger("P1"), passenger("P1")), List.of());
        mvc.perform(post("/api/evaluations/flight").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("El identificador de pasajero está duplicado: P1"));
    }

    @Test
    void duplicateSeatIdsAreRejected() throws Exception {
        FlightEvaluationRequest request = new FlightEvaluationRequest(new FlightInput("AV-101", 0, 180),
                List.of(passenger("P1")), List.of(
                new SeatInput("E1", SeatType.EMERGENCY_EXIT, false),
                new SeatInput("E1", SeatType.FAMILY, false)));
        mvc.perform(post("/api/evaluations/flight").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("El identificador de asiento está duplicado: E1"));
    }

    @Test
    void invalidJsonTypeReturnsClientError() throws Exception {
        mvc.perform(post("/api/evaluations/flight").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flight\":{\"delayMinutes\":\"mucho\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("JSON inválido o campo con tipo incorrecto"));
    }
}
