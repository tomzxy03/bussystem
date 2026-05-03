package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.PaymentTransactionStatus;
import com.tomzxy.busozy.common.enums.RefundStatus;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.CancelBookingReqDTO;
import com.tomzxy.busozy.dto.response.CancellationPreviewResDTO;
import com.tomzxy.busozy.dto.response.CancellationResDTO;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.entity.Cancellation;
import com.tomzxy.busozy.entity.CancellationPolicy;
import com.tomzxy.busozy.entity.Payment;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.event.RefundRequestedEvent;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.CancellationMapper;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.repository.CancellationPolicyRepository;
import com.tomzxy.busozy.repository.CancellationRepository;
import com.tomzxy.busozy.repository.PaymentRepository;
import com.tomzxy.busozy.repository.PromotionRepository;
import com.tomzxy.busozy.repository.UserPromotionUsageRepository;
import com.tomzxy.busozy.repository.UserRepository;
import com.tomzxy.busozy.service.interfaces.CancellationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancellationServiceImpl implements CancellationService {

    private static final Logger log = LoggerFactory.getLogger(CancellationServiceImpl.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String KEY_TRIP_SEATS = "trip:seats:";
    private static final String KEY_USER_BOOKINGS = "user:bookings:";

    private final BookingRepository bookingRepository;
    private final CancellationRepository cancellationRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final PaymentRepository paymentRepository;
    private final PromotionRepository promotionRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;
    private final UserRepository userRepository;
    private final CancellationMapper cancellationMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public CancellationPreviewResDTO getCancellationPreview(Long userId, UUID bookingCode) {
        Booking booking = loadOwnedBooking(userId, bookingCode);
        validateBookingCancelable(booking);

        OffsetDateTime departureTime = departureTimeOf(booking);
        if (booking.getPaymentStatus() != com.tomzxy.busozy.common.enums.BookingPaymentStatus.PAID) {
            return new CancellationPreviewResDTO(
                    booking.getId(),
                    booking.getBookingCode().toString(),
                    booking.getFinalPrice(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0D,
                    "Booking chưa thanh toán, hủy vé sẽ không phát sinh hoàn tiền.",
                    departureTime);
        }

        PolicyPreview preview = resolvePolicyPreview(booking);
        BigDecimal cappedRefundAmount = capRefundAmount(booking, preview.refundAmount());
        return new CancellationPreviewResDTO(
                booking.getId(),
                booking.getBookingCode().toString(),
                booking.getFinalPrice(),
                cappedRefundAmount,
                preview.refundPercentage().doubleValue(),
                preview.description(),
                departureTime);
    }

    @Override
    @Transactional
    public CancellationResDTO cancelBooking(Long userId, UUID bookingCode, CancelBookingReqDTO req) {
        Booking booking = loadOwnedBooking(userId, bookingCode);
        validateBookingCancelable(booking);

        cancellationRepository.findByBookingId(booking.getId()).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        });

        String sanitizedReason = sanitizeReason(req.getReason());
        User cancelledBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        boolean promotionUsageConsumed = booking.getPromotionId() != null
                && booking.getPaymentStatus() == com.tomzxy.busozy.common.enums.BookingPaymentStatus.PAID;

        BigDecimal refundAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        RefundStatus refundStatus = RefundStatus.COMPLETED;
        boolean shouldProcessRefund = false;

        if (booking.getPaymentStatus() == com.tomzxy.busozy.common.enums.BookingPaymentStatus.PAID) {
            PolicyPreview preview = resolvePolicyPreview(booking);
            refundAmount = capRefundAmount(booking, preview.refundAmount());
            shouldProcessRefund = refundAmount.compareTo(BigDecimal.ZERO) > 0;
            refundStatus = shouldProcessRefund ? RefundStatus.PENDING : RefundStatus.COMPLETED;
        } else {
            paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                if (payment.getStatus() == PaymentTransactionStatus.PENDING) {
                    payment.setStatus(PaymentTransactionStatus.CANCELLED);
                    paymentRepository.save(payment);
                }
            });
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Cancellation cancellation = new Cancellation();
        cancellation.setBooking(booking);
        cancellation.setCancelledBy(cancelledBy);
        cancellation.setCancelReason(sanitizedReason);
        cancellation.setCancelTime(OffsetDateTime.now(APP_ZONE));
        cancellation.setRefundAmount(refundAmount);
        cancellation.setRefundStatus(refundStatus);
        cancellationRepository.save(cancellation);

        rollbackPromotionUsageIfNeeded(booking, userId, promotionUsageConsumed);

        evictCaches(booking.getTrip().getId(), userId);

        if (shouldProcessRefund) {
            eventPublisher.publishEvent(new RefundRequestedEvent(cancellation.getId(), booking.getId(), refundAmount));
        }

        log.info("Booking cancelled with phase12 flow: bookingCode={}, refundAmount={}", booking.getBookingCode(), refundAmount);
        return cancellationMapper.toCancellationRes(cancellation);
    }

    @Override
    @Transactional(readOnly = true)
    public CancellationResDTO getCancellationDetail(Long userId, UUID bookingCode) {
        Booking booking = loadOwnedBooking(userId, bookingCode);
        Cancellation cancellation = cancellationRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANCELLATION_NOT_FOUND));
        return cancellationMapper.toCancellationRes(cancellation);
    }

    private Booking loadOwnedBooking(Long userId, UUID bookingCode) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND));
        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CANCELLATION_ACCESS_DENIED);
        }
        return booking;
    }

    private void validateBookingCancelable(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }
        if (booking.getStatus() == BookingStatus.EXPIRED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }
        if (departureTimeOf(booking).isBefore(OffsetDateTime.now(APP_ZONE))) {
            throw new BusinessException(ErrorCode.CANCELLATION_NOT_ALLOWED);
        }
    }

    private PolicyPreview resolvePolicyPreview(Booking booking) {
        OffsetDateTime departureTime = departureTimeOf(booking);
        long hoursUntilDeparture = Math.max(0L, ChronoUnit.HOURS.between(OffsetDateTime.now(APP_ZONE), departureTime));

        List<CancellationPolicy> specificPolicies = cancellationPolicyRepository.findActiveCompanyRoutePolicies(
                booking.getTrip().getRoute().getCompany().getId(),
                booking.getTrip().getRoute().getId());
        PolicyPreview preview = resolveFromPolicies(booking, specificPolicies, hoursUntilDeparture, "company+route");
        if (preview != null) {
            return preview;
        }

        List<CancellationPolicy> companyPolicies = cancellationPolicyRepository.findActiveCompanyPolicies(
                booking.getTrip().getRoute().getCompany().getId());
        preview = resolveFromPolicies(booking, companyPolicies, hoursUntilDeparture, "company");
        if (preview != null) {
            return preview;
        }

        List<CancellationPolicy> routePolicies = cancellationPolicyRepository.findActiveRoutePolicies(
                booking.getTrip().getRoute().getId());
        preview = resolveFromPolicies(booking, routePolicies, hoursUntilDeparture, "route");
        if (preview != null) {
            return preview;
        }

        List<CancellationPolicy> globalPolicies = cancellationPolicyRepository.findActiveGlobalPolicies();
        preview = resolveFromPolicies(booking, globalPolicies, hoursUntilDeparture, "global");
        if (preview != null) {
            return preview;
        }

        CancellationPolicy fallback = new CancellationPolicy();
        fallback.setHoursBeforeDeparture(24);
        fallback.setRefundPercentage(BigDecimal.valueOf(100));
        return buildPreview(booking, fallback, hoursUntilDeparture, "default");
    }

    private PolicyPreview resolveFromPolicies(
            Booking booking,
            List<CancellationPolicy> policies,
            long hoursUntilDeparture,
            String scope) {
        if (policies == null || policies.isEmpty()) {
            return null;
        }

        for (CancellationPolicy policy : policies) {
            if (hoursUntilDeparture >= policy.getHoursBeforeDeparture()) {
                return buildPreview(booking, policy, hoursUntilDeparture, scope);
            }
        }

        CancellationPolicy nearest = policies.get(policies.size() - 1);
        return new PolicyPreview(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                "Không đủ điều kiện hoàn tiền. Cần hủy trước ít nhất " + nearest.getHoursBeforeDeparture()
                        + " giờ theo policy " + scope + ".");
    }

    private PolicyPreview buildPreview(Booking booking, CancellationPolicy policy, long hoursUntilDeparture, String scope) {
        BigDecimal percentage = policy.getRefundPercentage().setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundAmount = booking.getFinalPrice()
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String description = "Hoàn " + percentage.stripTrailingZeros().toPlainString() + "% nếu hủy trước ít nhất "
                + policy.getHoursBeforeDeparture() + " giờ. Scope áp dụng: " + scope + ". Còn "
                + hoursUntilDeparture + " giờ trước giờ khởi hành.";
        return new PolicyPreview(refundAmount, percentage, description);
    }

    private OffsetDateTime departureTimeOf(Booking booking) {
        return booking.getTrip().getDepartureTime().atDate(booking.getTrip().getDepartureDate());
    }

    private String sanitizeReason(String reason) {
        String sanitized = reason == null ? "" : reason.trim();
        if (sanitized.contains("<") || sanitized.contains(">")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Lý do hủy vé không được chứa HTML/JS");
        }
        return sanitized;
    }

    private void evictCaches(Long tripId, Long userId) {
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_SEATS + tripId);
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_USER_BOOKINGS + userId);
    }

    private BigDecimal capRefundAmount(Booking booking, BigDecimal calculatedRefund) {
        if (calculatedRefund == null || calculatedRefund.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal maxRefund = paymentRepository.findByBookingId(booking.getId())
                .map(Payment::getAmount)
                .orElse(booking.getFinalPrice());
        return calculatedRefund.min(maxRefund).setScale(2, RoundingMode.HALF_UP);
    }

    private void rollbackPromotionUsageIfNeeded(Booking booking, Long userId, boolean promotionUsageConsumed) {
        if (!promotionUsageConsumed) {
            return;
        }

        int promotionUpdated = promotionRepository.decrementUsedCount(booking.getPromotionId());
        int userUsageUpdated = userPromotionUsageRepository.decrementUsage(userId, booking.getPromotionId());
        if (promotionUpdated == 0 || userUsageUpdated == 0) {
            log.warn("Promotion usage rollback was partial for bookingCode={}, promotionId={}, promoUpdated={}, userUsageUpdated={}",
                    booking.getBookingCode(), booking.getPromotionId(), promotionUpdated, userUsageUpdated);
        }
    }

    private record PolicyPreview(
            BigDecimal refundAmount,
            BigDecimal refundPercentage,
            String description) {
    }
}
