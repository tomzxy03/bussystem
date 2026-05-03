package com.tomzxy.busozy.event;

import com.tomzxy.busozy.service.impl.CancellationRefundProcessor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RefundEventListener {

    private static final Logger log = LoggerFactory.getLogger(RefundEventListener.class);

    private final CancellationRefundProcessor refundProcessor;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundRequested(RefundRequestedEvent event) {
        try {
            refundProcessor.processRefund(event.cancellationId());
        } catch (Exception ex) {
            log.error("Async refund processing failed for cancellationId={}", event.cancellationId(), ex);
        }
    }
}
