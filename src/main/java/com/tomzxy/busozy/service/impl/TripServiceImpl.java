package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.SeatAvailabilityDTO;
import com.tomzxy.busozy.dto.response.TripDetailResDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.entity.*;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.TripMapper;
import com.tomzxy.busozy.repository.*;
import com.tomzxy.busozy.service.interfaces.TripService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

    private final TripRepository tripRepository;
    private final TripSegmentRepository tripSegmentRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final RoutePriceRepository routePriceRepository;
    private final SeatRepository seatRepository;
    private final TripMapper tripMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    @PersistenceContext
    private EntityManager em;

    // Redis key: busozy:{env}:trip:seats:{tripId} TTL 15m
    private static final String KEY_TRIP_SEATS = "trip:seats:";
    private static final Duration TTL_15M = Duration.ofMinutes(15);

    // ─────────────────────────────────────────────
    // PUBLIC
    // ─────────────────────────────────────────────

    @Override
    public Page<TripResDTO> searchTrips(Long originStopId, Long destStopId,
            java.time.LocalDate date, Integer passengers,
            Pageable pageable) {
        return tripRepository.searchAvailable(originStopId, destStopId, date, pageable)
                .map(tripMapper::toTripRes);
    }

    @Override
    public TripDetailResDTO getTripDetail(Long id) {
        Trip trip = requireTrip(id);
        List<TripDetailResDTO.TripSegmentResDTO> segments = trip.getSegments().stream().map(tripMapper::toSegmentRes)
                .toList();

        // Derive price range from route prices for quick display
        BigDecimal minPrice = BigDecimal.ZERO;
        BigDecimal maxPrice = BigDecimal.ZERO;
        List<RoutePrice> prices = routePriceRepository
                .findByRouteIdOrderByPickupOrderAscDropoffOrderAsc(trip.getRoute().getId());
        if (!prices.isEmpty()) {
            minPrice = prices.stream().map(RoutePrice::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            maxPrice = prices.stream().map(RoutePrice::getPrice).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        }
        return new TripDetailResDTO(tripMapper.toTripRes(trip), segments, minPrice, maxPrice);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SeatAvailabilityDTO> getTripSeats(Long tripId, Integer pickupOrder, Integer dropoffOrder) {
        // Validate pickup < dropoff if provided
        if (pickupOrder != null && dropoffOrder != null && pickupOrder >= dropoffOrder) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_SEGMENT);
        }

        String key = redisConfig.keyPrefix() + KEY_TRIP_SEATS + tripId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?> list) {
            log.debug("Cache hit: trip seats {}", tripId);
            return (List<SeatAvailabilityDTO>) list;
        }

        Trip trip = requireTrip(tripId);
        Long busId = trip.getBus().getId();

        /*
         * Dynamic availability query per spec:
         * "Availability query dùng NOT (dropoff_order <= pickup OR pickup_order >= dropoff)"
         * booking_seats and bookings tables don't exist yet (Phase 7+), so we return
         * all
         * seats as AVAILABLE — the structure is ready for Phase 7 to add the LEFT JOIN.
         */
        List<Seat> allSeats = seatRepository.findByBusIdAndIsActiveTrueOrderByRowNumAscColNumAsc(busId);
        List<SeatAvailabilityDTO> result = allSeats.stream()
                .map(s -> new SeatAvailabilityDTO(
                        s.getId(),
                        s.getSeatNumber(),
                        s.getSeatType() != null ? s.getSeatType().name() : "STANDARD",
                        s.getRowNum(),
                        s.getColNum(),
                        s.getPriceMultiplier(),
                        "AVAILABLE" // Phase 7 will replace this with DB LEFT JOIN result
                )).toList();

        redisTemplate.opsForValue().set(key, result, TTL_15M);
        return result;
    }

    private Trip requireTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRIP_NOT_FOUND));
    }

    /** Evicts the 15-minute Redis seat status cache for a trip. */
    public void evictTripSeatsCache(Long tripId) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_SEATS + tripId);
    }
}
