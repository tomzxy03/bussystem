package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full detail DTO for a single trip, including its segments and price range.
 * Price range is derived from route_prices for the matched pickup/dropoff
 * range.
 */
public record TripDetailResDTO(
        TripResDTO trip,
        List<TripSegmentResDTO> segments,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
    public record TripSegmentResDTO(
            Integer pickupOrder,
            Integer dropoffOrder,
            Integer totalSeats,
            Integer availableSeats) {
    }
}
