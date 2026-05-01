package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.entity.*;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.TripMapper;
import com.tomzxy.busozy.repository.*;
import com.tomzxy.busozy.service.interfaces.admin.AdminTripService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTripServiceImpl implements AdminTripService {

    private static final Logger log = LoggerFactory.getLogger(AdminTripServiceImpl.class);

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final DriverRepository driverRepository;
    private final RoutePriceRepository routePriceRepository;
    private final SeatRepository seatRepository;
    private final TripMapper tripMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    private static final String KEY_TRIP_SEATS = "trip:seats:";

    @Override
    @Transactional
    public TripResDTO createTrip(TripCreateReqDTO req) {
        Route route = routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND));
        Bus bus = busRepository.findById(req.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_NOT_FOUND));

        if (tripRepository.existsBusConflict(req.getBusId(), req.getDepartureDate(), -1L)) {
            throw new BusinessException(ErrorCode.BUS_TIME_CONFLICT);
        }

        Driver driver = null;
        if (req.getDriverId() != null) {
            driver = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DRIVER_NOT_FOUND));
            if (!driver.getCompany().getId().equals(route.getCompany().getId())) {
                throw new BusinessException(ErrorCode.DRIVER_COMPANY_MISMATCH);
            }
        }

        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setBus(bus);
        trip.setDriver(driver);
        trip.setDepartureDate(req.getDepartureDate());
        trip.setDepartureTime(req.getDepartureTime());
        trip.setStatus(TripStatus.DRAFT);

        initSegmentsFromRoutePrices(trip, route, bus);

        tripRepository.save(trip);
        log.info("Trip created: id={}, route={}, bus={}", trip.getId(), route.getCode(), bus.getLicensePlate());
        return tripMapper.toTripRes(trip);
    }

    @Override
    @Transactional
    public TripResDTO updateTrip(Long id, TripCreateReqDTO req) {
        Trip trip = requireTrip(id);

        Route route = routeRepository.findById(req.getRouteId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROUTE_NOT_FOUND));
        Bus bus = busRepository.findById(req.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BUS_NOT_FOUND));

        if (tripRepository.existsBusConflict(req.getBusId(), req.getDepartureDate(), id)) {
            throw new BusinessException(ErrorCode.BUS_TIME_CONFLICT);
        }

        Driver driver = null;
        if (req.getDriverId() != null) {
            driver = driverRepository.findById(req.getDriverId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.DRIVER_NOT_FOUND));
            if (!driver.getCompany().getId().equals(route.getCompany().getId())) {
                throw new BusinessException(ErrorCode.DRIVER_COMPANY_MISMATCH);
            }
        }

        trip.setRoute(route);
        trip.setBus(bus);
        trip.setDriver(driver);
        trip.setDepartureDate(req.getDepartureDate());
        trip.setDepartureTime(req.getDepartureTime());

        trip.getSegments().clear();
        initSegmentsFromRoutePrices(trip, route, bus);

        tripRepository.save(trip);
        evictTripSeatsCache(id);
        return tripMapper.toTripRes(trip);
    }

    @Override
    @Transactional
    public TripResDTO updateTripStatus(Long id, TripStatus status) {
        Trip trip = requireTrip(id);
        if (!trip.getStatus().canTransitionTo(status)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể chuyển từ " + trip.getStatus() + " sang " + status);
        }
        trip.setStatus(status);
        tripRepository.save(trip);

        log.info("Trip {} status changed: {} → {}", id, trip.getStatus(), status);
        if (status == TripStatus.CANCELLED || status == TripStatus.COMPLETED) {
            evictTripSeatsCache(id);
        }
        return tripMapper.toTripRes(trip);
    }

    @Override
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = requireTrip(id);
        if (trip.getStatus() != TripStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SCHEDULED_TRIP);
        }
        trip.setDeletedAt(OffsetDateTime.now());
        tripRepository.save(trip);
        evictTripSeatsCache(id);
        log.info("Trip soft-deleted: id={}", id);
    }

    private Trip requireTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void initSegmentsFromRoutePrices(Trip trip, Route route, Bus bus) {
        int totalSeats = (int) seatRepository.countActiveByBusId(bus.getId());
        List<RoutePrice> prices = routePriceRepository
                .findByRouteIdOrderByPickupOrderAscDropoffOrderAsc(route.getId());

        List<TripSegment> segments = new ArrayList<>(prices.size());
        for (RoutePrice rp : prices) {
            TripSegment seg = new TripSegment();
            seg.setTrip(trip);
            seg.setPickupOrder(rp.getPickupOrder());
            seg.setDropoffOrder(rp.getDropoffOrder());
            seg.setTotalSeats(totalSeats);
            seg.setAvailableSeats(totalSeats);
            segments.add(seg);
        }
        trip.getSegments().addAll(segments);
    }

    private void evictTripSeatsCache(Long tripId) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_SEATS + tripId);
    }
}
