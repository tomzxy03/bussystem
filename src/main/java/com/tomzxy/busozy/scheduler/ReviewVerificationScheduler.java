package com.tomzxy.busozy.scheduler;

import com.tomzxy.busozy.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewVerificationScheduler {

    private final ReviewRepository reviewRepository;

    @Scheduled(fixedDelay = 86_400_000)
    @Transactional
    public void autoVerifyReviews() {
        int updated = reviewRepository.autoVerifyOlderThan(OffsetDateTime.now().minusHours(24));
        if (updated > 0) {
            log.info("Auto-verified {} reviews", updated);
        }
    }
}
