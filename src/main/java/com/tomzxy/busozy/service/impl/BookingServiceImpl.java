package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.BookingPaymentStatus;
import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.CreateBookingReqDTO;
import com.tomzxy.busozy.dto.request.PassengerReqDTO;
import com.tomzxy.busozy.dto.response.BookingDetailResDTO;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.entity.*;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.BookingMapper;
import com.tomzxy.busozy.repository.*;
import com.tomzxy.busozy.service.interfaces.BookingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final RoutePriceRepository routePriceRepository;
    private final BookingMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Redis keys
    private static final String KEY_IDEMPOTENCY = "idempotency:booking:";
    private static final String KEY_LOCK_TRIP = "lock:trip:";
    private static final String KEY_TRIP_SEATS = "trip:seats:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final Duration IDEM_TTL = Duration.ofHours(24);
    private static final Duration HOLD_TTL = Duration.ofMinutes(10);

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: CREATE BOOKING (Qwen-reviewed: lock + idempotency OUTSIDE
    // @Transactional)
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public BookingResDTO createBooking(Long userId, CreateBookingReqDTO req, String idempotencyKey) {
        String idemKey = redisConfig.keyPrefix() + KEY_IDEMPOTENCY + idempotencyKey;
        String lockKey = redisConfig.keyPrefix() + KEY_LOCK_TRIP + req.getTripId();

        // Step 1: Idempotency check — return cached result immediately (no lock needed)
        Object cached = redisTemplate.opsForValue().get(idemKey);
        if (cached instanceof BookingResDTO dto) {
            log.debug("Idempotency hit for key={}", idempotencyKey);
            return dto;
        }

        // Step 2: Acquire distributed lock per trip (fail-fast, 3s TTL)
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            throw new ConflictException(ErrorCode.SEAT_LOCKED);
        }

        try {
            // Step 3-5: Transactional core (overlap check → price → persist)
            BookingResDTO result = createBookingTransactional(userId, req);

            // Step 6: Cache idempotency result (24h) — after commit
            redisTemplate.opsForValue().set(idemKey, result, IDEM_TTL);

            // Step 7: Invalidate trip seat cache
            redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_SEATS + req.getTripId());

            return result;
        } finally {
            // Always release lock, even on rollback
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional
    public BookingResDTO createBookingTransactional(Long userId, CreateBookingReqDTO req) {
        // --- Validate trip ---
        Trip trip = tripRepository.findById(req.getTripId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_ACTIVE));
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new BusinessException(ErrorCode.TRIP_NOT_ACTIVE);
        }
        if (trip.getDepartureDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.TRIP_NOT_ACTIVE);
        }
        // pickup < dropoff (defend in code even though DB has CHECK constraint)
        if (req.getPickupOrder() >= req.getDropoffOrder()) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_SEGMENT);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));

        // --- Resolve seats by seatNumber (row+col) within the bus ---
        Long busId = trip.getBus().getId();
        List<String> seatNumbers = req.getPassengers().stream()
                .map(PassengerReqDTO::toSeatNumber)
                .toList();
        List<Seat> resolvedSeats = seatRepository
                .findByBusIdAndSeatNumberIn(busId, seatNumbers);
        if (resolvedSeats.size() != seatNumbers.size()) {
            throw new BusinessException(ErrorCode.INVALID_SEAT_LAYOUT); // seat not found on bus
        }
        List<Long> seatIds = resolvedSeats.stream().map(Seat::getId).toList();

        // --- Overlap check (source of truth, native query) ---
        List<Long> overlapping = bookingRepository.findOverlappingSeats(
                req.getTripId(), seatIds, req.getPickupOrder(), req.getDropoffOrder());
        if (!overlapping.isEmpty()) {
            throw new ConflictException(ErrorCode.SEATS_FULL_OR_OVERLAP);
        }

        // --- Price calculation (strict server-side, no FE trust) ---
        RoutePrice routePrice = routePriceRepository
                .findByRouteIdAndPickupOrderAndDropoffOrder(
                        trip.getRoute().getId(), req.getPickupOrder(), req.getDropoffOrder())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROUTE_PRICE_NOT_FOUND));
        BigDecimal basePrice = routePrice.getPrice();

        // --- Build Booking ---
        Booking booking = new Booking();
        booking.setBookingCode(UUID.randomUUID());
        booking.setUser(user);
        booking.setTrip(trip);
        booking.setPickupOrder(req.getPickupOrder());
        booking.setDropoffOrder(req.getDropoffOrder());
        booking.setBasePrice(basePrice);
        booking.setStatus(com.tomzxy.busozy.common.enums.BookingStatus.PENDING);
        booking.setPaymentStatus(com.tomzxy.busozy.common.enums.BookingPaymentStatus.PENDING);
        booking.setReservedUntil(OffsetDateTime.now().plus(HOLD_TTL));

        // --- Build BookingSeats + Passengers ---
        BigDecimal total = BigDecimal.ZERO;
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (int i = 0; i < resolvedSeats.size(); i++) {
            Seat seat = resolvedSeats.get(i);
            PassengerReqDTO passengerDto = req.getPassengers().get(i);

            BigDecimal multiplier = seat.getPriceMultiplier() != null
                    ? seat.getPriceMultiplier()
                    : BigDecimal.ONE;
            BigDecimal seatFinalPrice = basePrice.multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP);
            total = total.add(seatFinalPrice);

            BookingSeat bookingSeat = new BookingSeat();
            bookingSeat.setBooking(booking);
            bookingSeat.setSeat(seat);
            bookingSeat.setFinalPrice(seatFinalPrice);
            bookingSeat.setCurrency("VND");

            Passenger passenger = new Passenger();
            passenger.setBookingSeat(bookingSeat);
            passenger.setFullName(passengerDto.getFullName());
            passenger.setPhone(passengerDto.getPhone());
            passenger.setIdCard(passengerDto.getIdCard());
            passenger.setDateOfBirth(passengerDto.getDateOfBirth());
            bookingSeat.setPassenger(passenger);

            bookingSeats.add(bookingSeat);
        }
        booking.setFinalPrice(total.setScale(2, RoundingMode.HALF_UP));
        booking.getSeats().addAll(bookingSeats);

        bookingRepository.save(booking); // cascades to BookingSeat + Passenger
        log.info("Booking created: code={}, trip={}, user={}, total={}",
                booking.getBookingCode(), req.getTripId(), userId, total);

        return mapper.toBookingRes(booking);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: MY BOOKINGS
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public Page<BookingResDTO> getMyBookings(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        return bookingRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(mapper::toBookingRes);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: BOOKING DETAIL
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public BookingDetailResDTO getBookingDetail(Long userId, UUID bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND));
        // Ownership check
        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        BookingResDTO summary = mapper.toBookingRes(booking);
        List<BookingDetailResDTO.BookingSeatResDTO> seats = booking.getSeats().stream()
                .map(mapper::toSeatRes).toList();
        return new BookingDetailResDTO(summary, seats);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: CANCEL
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public BookingResDTO cancelBooking(Long userId, UUID bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        // Only PENDING or CONFIRMED bookings can be cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.EXPIRED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_CANCEL_STATUS);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Invalidate trip seat cache so others see freed seats immediately
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_SEATS + booking.getTrip().getId());
        log.info("Booking cancelled: code={}", bookingCode);
        return mapper.toBookingRes(booking);
    }

    // Replaced ApplicationContext with ApplicationEventPublisher
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────────────────────────────
    // INTERNAL: Called by PaymentService after PAID callback
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void confirmPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(BookingPaymentStatus.PAID);
        bookingRepository.save(booking);
        // Evict seat cache — freed hold becomes a confirmed seat
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_SEATS + booking.getTrip().getId());

        // Phase 9 Promotion logic: delegate cleanly via Spring Event
        eventPublisher.publishEvent(new com.tomzxy.busozy.event.BookingConfirmedEvent(
                booking.getId(),
                booking.getPromotionId(),
                booking.getUser().getId()));

        log.info("Booking confirmed after payment: bookingCode={}", booking.getBookingCode());
    }
}
