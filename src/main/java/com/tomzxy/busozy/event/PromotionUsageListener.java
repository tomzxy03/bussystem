package com.tomzxy.busozy.event;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.repository.UserPromotionUsageRepository;
import com.tomzxy.busozy.service.interfaces.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class PromotionUsageListener {

    private final PromotionService promotionService;
    private final UserPromotionUsageRepository userUsageRepo;

    /**
     * Runs synchronously within the confirmPayment transaction.
     * If the limit is reached, it throws an exception which triggers a rollback.
     */
    @EventListener
    @Transactional
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        if (event.promotionId() != null) {
            boolean success = promotionService.incrementUsage(event.promotionId());
            if (!success) {
                // Fail fast and rollback the confirmation and payment if the limit is filled
                // concurrently
                throw new BusinessException(ErrorCode.PROMOTION_USAGE_EXCEEDED);
            }

            OffsetDateTime nowVn = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            userUsageRepo.upsertUsage(event.userId(), event.promotionId(), nowVn);
        }
    }
}
