package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.dto.response.DashboardResDTO;
import com.tomzxy.busozy.dto.response.RouteStatsResDTO;
import com.tomzxy.busozy.mapper.BookingMapper;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.repository.TripRepository;
import com.tomzxy.busozy.service.interfaces.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final BookingMapper bookingMapper;

    @Override
    public DashboardResDTO getGlobalDashboard() {
        LocalDate today = LocalDate.now();
        BookingRepository.DashboardStatsProjection stats = bookingRepository.getTodayStats(today);
        List<RouteStatsResDTO> topRoutes = bookingRepository.findTopRouteStats(today, PageRequest.of(0, 5))
                .stream()
                .map(route -> new RouteStatsResDTO(route.getRouteName(), route.getTotalBookings(), route.getTotalRevenue()))
                .toList();

        return new DashboardResDTO(
                stats != null && stats.getTotalBookingsToday() != null ? stats.getTotalBookingsToday() : 0L,
                stats != null && stats.getTotalRevenueToday() != null ? stats.getTotalRevenueToday() : java.math.BigDecimal.ZERO,
                tripRepository.countByStatusIn(List.of(TripStatus.SCHEDULED, TripStatus.DELAYED, TripStatus.DEPARTED)),
                stats != null && stats.getPendingBookingsCount() != null ? stats.getPendingBookingsCount() : 0L,
                topRoutes,
                bookingRepository.findRecentBookings(PageRequest.of(0, 5)).stream().map(bookingMapper::toBookingRes).toList());
    }
}
