package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.PaymentInitiateReqDTO;
import com.tomzxy.busozy.dto.response.PaymentInitiateResDTO;
import com.tomzxy.busozy.dto.response.PaymentResDTO;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.entity.Payment;
import com.tomzxy.busozy.entity.PaymentMethod;
import com.tomzxy.busozy.event.PaymentSuccessEvent;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.PaymentMapper;
import com.tomzxy.busozy.payment.GatewayFactory;
import com.tomzxy.busozy.payment.GatewayInitiateResponse;
import com.tomzxy.busozy.payment.PaymentGatewayProvider;
import com.tomzxy.busozy.payment.PaymentProcessResult;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.repository.PaymentMethodRepository;
import com.tomzxy.busozy.repository.PaymentRepository;
import com.tomzxy.busozy.service.interfaces.BookingService;
import com.tomzxy.busozy.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final GatewayFactory gatewayFactory;
    private final PaymentMapper paymentMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    private static final String KEY_TRIP_SEATS = "trip:seats:";
    private static final String MOCK_RETURN_URL = "https://busozy.vn/payment/result";

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: Active payment methods
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public List<PaymentMethod> getActiveMethods() {
        return paymentMethodRepository.findByIsActiveTrueOrderByIdAsc();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: Initiate payment (6-step)
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentInitiateResDTO initiatePayment(Long userId, PaymentInitiateReqDTO req) {

        // Step 1: Validate booking ownership + payable state
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_BOOKING_INVALID));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (booking.getStatus() != com.tomzxy.busozy.common.enums.BookingStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_BOOKING_INVALID);
        }
        if (booking.getPaymentStatus() != com.tomzxy.busozy.common.enums.BookingPaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        if (booking.getReservedUntil() != null && booking.getReservedUntil().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.PAYMENT_BOOKING_INVALID); // seat hold expired
        }

        // Step 2: Validate method is active
        PaymentMethod method = paymentMethodRepository
                .findByCodeAndIsActiveTrue(req.getMethodCode().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_UNSUPPORTED));

        // Step 3: Idempotency — only one PENDING payment per booking
        paymentRepository.findByBookingId(booking.getId()).ifPresent(existing -> {
            if (existing.getStatus() != com.tomzxy.busozy.common.enums.PaymentTransactionStatus.CANCELLED
                    && existing.getStatus() != com.tomzxy.busozy.common.enums.PaymentTransactionStatus.FAILED) {
                throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
            }
        });

        // Step 4: Create Payment record (amount = booking.finalPrice — never
        // FE-supplied)
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(method);
        payment.setAmount(booking.getFinalPrice()); // strict server-side — spec PAY_005
        payment.setCurrency(booking.getCurrency());
        payment.setStatus(com.tomzxy.busozy.common.enums.PaymentTransactionStatus.PENDING);
        paymentRepository.save(payment);

        // Step 5: Route by method
        PaymentGatewayProvider provider = gatewayFactory.getProvider(method.getCode());
        GatewayInitiateResponse gatewayRes = provider.initiate(payment, MOCK_RETURN_URL);

        if ("COD".equals(method.getCode())) {
            // COD: instant confirm — no redirect needed
            payment.setStatus(com.tomzxy.busozy.common.enums.PaymentTransactionStatus.PAID);
            payment.setGatewayTransactionId("COD-" + payment.getId());
            paymentRepository.save(payment);
            bookingService.confirmPayment(booking.getId());
            log.info("COD payment confirmed: paymentId={}, bookingCode={}", payment.getId(), booking.getBookingCode());
        }

        // Step 6: Invalidate seat cache (done inside confirmPayment for COD; for
        // BANK_TRANSFER it's done on webhook)
        return new PaymentInitiateResDTO(
                payment.getId(),
                booking.getBookingCode(),
                payment.getAmount(),
                gatewayRes.paymentUrl(),
                gatewayRes.qrCodeData(),
                gatewayRes.directPayUrl());
    }

    // ─────────────────────────────────────────────────────────────────────
    // WEBHOOK: Strict idempotency + signature verify
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void handleWebhook(String provider, Map<String, String> params) {

        // 1. Resolve provider + verify HMAC signature
        PaymentGatewayProvider gateway = gatewayFactory.getProvider(provider);
        if (!gateway.verifySignature(params)) {
            log.warn("Webhook signature invalid for provider={}", provider);
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_SIGNATURE);
        }

        // 2. Idempotency: if already processed, return 200 immediately (no retry)
        String txId = params.get("transactionId");
        if (txId != null) {
            paymentRepository.findByGatewayTransactionId(txId).ifPresent(existing -> {
                if (existing.getStatus() != com.tomzxy.busozy.common.enums.PaymentTransactionStatus.PENDING) {
                    log.debug("Webhook idempotency hit: transactionId={}", txId);
                    throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
                }
            });
        }

        // 3. Process callback
        PaymentProcessResult result = gateway.processCallback(params);

        // 4. Find payment by transactionId or extract from params
        String bookingRef = params.get("bookingId");
        Payment payment = null;
        if (txId != null) {
            payment = paymentRepository.findByGatewayTransactionId(txId).orElse(null);
        }
        if (payment == null && bookingRef != null) {
            payment = paymentRepository.findByBookingId(Long.parseLong(bookingRef)).orElse(null);
        }
        if (payment == null) {
            log.warn("Webhook received for unknown transaction: params={}", params);
            return; // Ignore unknown — still return 200 to gateway
        }

        // 5. Update Payment
        payment.setStatus(result.success() ? com.tomzxy.busozy.common.enums.PaymentTransactionStatus.PAID : com.tomzxy.busozy.common.enums.PaymentTransactionStatus.FAILED);
        payment.setGatewayTransactionId(result.transactionId());
        // Store raw callback as Map<String,Object> for JSONB
        Map<String, Object> raw = new HashMap<>(params);
        payment.setGatewayResponse(raw);
        paymentRepository.save(payment);

        // 6. If PAID → confirm booking + async event
        if (result.success()) {
            bookingService.confirmPayment(payment.getBooking().getId());
            eventPublisher.publishEvent(new PaymentSuccessEvent(
                    payment.getId(),
                    payment.getBooking().getId(),
                    payment.getBooking().getUser().getId(),
                    payment.getBooking().getBookingCode().toString()));
            log.info("Payment PAID via {}: paymentId={}", provider, payment.getId());
        } else {
            log.info("Payment FAILED via {}: paymentId={}", provider, payment.getId());
        }
        // Webhook MUST return 200 after this — controller does not throw
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: Cancel pending payment
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentResDTO cancelPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!payment.getBooking().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (payment.getStatus() != com.tomzxy.busozy.common.enums.PaymentTransactionStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        payment.setStatus(com.tomzxy.busozy.common.enums.PaymentTransactionStatus.CANCELLED);
        paymentRepository.save(payment);
        log.info("Payment cancelled: paymentId={}", paymentId);
        return paymentMapper.toStatusRes(payment);
    }

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC: Status check
    // ─────────────────────────────────────────────────────────────────────

    @Override
    public PaymentResDTO getPaymentStatus(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!payment.getBooking().getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return paymentMapper.toStatusRes(payment);
    }
}
