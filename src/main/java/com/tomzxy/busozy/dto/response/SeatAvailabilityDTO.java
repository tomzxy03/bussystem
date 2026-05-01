package com.tomzxy.busozy.dto.response;

/**
 * Represents the live status of a seat on a specific trip (for the seat
 * picker).
 * Status is computed dynamically by the DB query — not cached in seats table.
 */
public record SeatAvailabilityDTO(
        Long seatId,
        String seatNumber,
        String seatType,
        Integer rowNum,
        Integer colNum,
        java.math.BigDecimal priceMultiplier,
        String status // "AVAILABLE" | "BOOKED"
) {
}
