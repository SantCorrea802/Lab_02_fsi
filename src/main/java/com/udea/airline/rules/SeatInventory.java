package com.udea.airline.rules;

import com.udea.airline.api.SeatInput;
import com.udea.airline.api.SeatResult;
import com.udea.airline.api.SeatType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SeatInventory {
    private final List<SeatState> seats;

    public SeatInventory(List<SeatInput> input) {
        seats = new ArrayList<>();
        for (SeatInput seat : input) {
            seats.add(new SeatState(seat.id(), seat.type(), seat.occupied()));
        }
        seats.sort(Comparator.comparing(seat -> seat.id));
    }

    public boolean isAvailableEmergencySeat() {
        return seats.stream().anyMatch(seat -> seat.type == SeatType.EMERGENCY_EXIT && !seat.occupied);
    }

    public String reserveFamilySeat(String passengerId) {
        return reserve(passengerId, SeatType.FAMILY);
    }

    public String reserveEmergencySeat(String passengerId) {
        return reserve(passengerId, SeatType.EMERGENCY_EXIT);
    }

    private String reserve(String passengerId, SeatType type) {
        // El inventario pertenece a una sola evaluación. Esta comprobación mantiene
        // la asignación única aunque varias activaciones coincidan al mismo tiempo.
        if (seats.stream().anyMatch(seat -> passengerId.equals(seat.assignedPassengerId))) {
            return null;
        }
        for (SeatState seat : seats) {
            if (seat.type == type && !seat.occupied) {
                seat.occupied = true;
                seat.assignedPassengerId = passengerId;
                return seat.id;
            }
        }
        return null;
    }

    public List<SeatResult> results() {
        return seats.stream()
                .map(seat -> new SeatResult(seat.id, seat.type, seat.occupied, seat.assignedPassengerId))
                .toList();
    }

    private static final class SeatState {
        private final String id;
        private final SeatType type;
        private boolean occupied;
        private String assignedPassengerId;

        private SeatState(String id, SeatType type, boolean occupied) {
            this.id = id;
            this.type = type;
            this.occupied = occupied;
        }
    }
}
