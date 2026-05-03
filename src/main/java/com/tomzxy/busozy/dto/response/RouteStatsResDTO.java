package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;

public record RouteStatsResDTO(
        String routeName,
        Long totalBookings,
        BigDecimal totalRevenue) {
}
