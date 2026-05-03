package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResDTO(
        long totalBookingsToday,
        BigDecimal totalRevenueToday,
        long activeTripsCount,
        long pendingBookingsCount,
        List<RouteStatsResDTO> topRoutes,
        List<BookingResDTO> recentBookings) {
}
