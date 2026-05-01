package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full route detail including ordered stops and price segments.
 * Nested records follow the spec convention.
 */
public record RouteDetailResDTO(
        RouteResDTO route,
        List<RouteStopResDTO> stops,
        List<RoutePriceResDTO> prices) {
    public record RouteStopResDTO(
            Integer stopOrder,
            String stopName,
            String provinceName,
            Integer minutesFromOrigin,
            BigDecimal distanceFromOrigin,
            Boolean isPickup,
            Boolean isDropoff) {
    }

    public record RoutePriceResDTO(
            Integer pickupOrder,
            Integer dropoffOrder,
            BigDecimal price,
            String currency) {
    }
}
