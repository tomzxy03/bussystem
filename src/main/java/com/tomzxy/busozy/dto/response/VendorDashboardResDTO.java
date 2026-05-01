package com.tomzxy.busozy.dto.response;

import java.math.BigDecimal;

public record VendorDashboardResDTO(
        int activeTrips,
        int activeBuses,
        int totalDrivers,
        BigDecimal todayRevenue,
        int pendingBookings) {

}
