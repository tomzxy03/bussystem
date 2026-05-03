package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.BookingPaymentStatus;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.PaymentTransactionStatus;
import com.tomzxy.busozy.common.enums.RefundStatus;
import com.tomzxy.busozy.entity.Cancellation;
import com.tomzxy.busozy.entity.Payment;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.payment.GatewayFactory;
import com.tomzxy.busozy.payment.GatewayRefundResponse;
import com.tomzxy.busozy.payment.PaymentGatewayProvider;
import com.tomzxy.busozy.repository.CancellationRepository;
import com.tomzxy.busozy.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CancellationRefundProcessor {

    private static final Logger log = LoggerFactory.getLogger(CancellationRefundProcessor.class);

    private final CancellationRepository cancellationRepository;
    private final PaymentRepository paymentRepository;
    private final GatewayFactory gatewayFactory;

    @Transactional(noRollbackFor = BusinessException.class)
    public void processRefund(Long cancellationId) {
        Cancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANCELLATION_NOT_FOUND));

        if (cancellation.getRefundAmount() == null || cancellation.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            cancellation.setRefundStatus(RefundStatus.COMPLETED);
            cancellationRepository.save(cancellation);
            return;
        }
        if (cancellation.getRefundStatus() == RefundStatus.COMPLETED) {
            return;
        }

        Payment payment = paymentRepository.findByBookingId(cancellation.getBooking().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_PROCESSING_FAILED, "Không tìm thấy giao dịch để hoàn tiền"));

        try {
            PaymentGatewayProvider provider = gatewayFactory.getProvider(payment.getPaymentMethod().getCode());
            GatewayRefundResponse response = provider.refund(
                    payment,
                    cancellation.getRefundAmount(),
                    "Refund for booking " + cancellation.getBooking().getBookingCode());

            cancellation.setRefundStatus(RefundStatus.COMPLETED);
            cancellation.setGatewayRefundId(response.transactionId());

            payment.setStatus(PaymentTransactionStatus.REFUNDED);
            payment.setGatewayResponse(mergeGatewayResponse(payment.getGatewayResponse(), response));
            cancellation.getBooking().setPaymentStatus(BookingPaymentStatus.REFUNDED);

            paymentRepository.save(payment);
            cancellationRepository.save(cancellation);
            log.info("Refund completed for cancellationId={}, bookingId={}", cancellationId, cancellation.getBooking().getId());
        } catch (Exception ex) {
            cancellation.setRefundStatus(RefundStatus.FAILED);
            cancellationRepository.save(cancellation);
            log.error("Refund failed for cancellationId={}", cancellationId, ex);
            throw new BusinessException(ErrorCode.REFUND_PROCESSING_FAILED);
        }
    }

    private Map<String, Object> mergeGatewayResponse(Map<String, Object> current, GatewayRefundResponse response) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (current != null) {
            merged.putAll(current);
        }
        merged.put("refundTransactionId", response.transactionId());
        merged.put("refundResponse", response.rawResponse());
        return merged;
    }
}
