package com.tomzxy.busozy.scheduler;

import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job that expires PENDING bookings whose seat-hold TTL has elapsed.
 *
 * Follows Qwen's reviewed pattern:
 * 1. Bulk UPDATE (single query, not N+1).
 * 2. Targeted cache invalidation only for affected tripIds.
 */
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingRepository bookingRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    private static final String KEY_TRIP_SEATS = "trip:seats:";

    /**
     * Runs every 60 seconds.
     * 1. Batch-expire overdue PENDING bookings (single UPDATE).
     * 2. Collect affected tripIds and evict their seat caches.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingBookings() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowStart = now.minusMinutes(2); // look back 2m for recently expired

        int updated = bookingRepository.batchExpirePending(now);

        if (updated > 0) {
            log.info("Expired {} pending booking(s)", updated);
            // Fetch tripIds of rows we just expired to invalidate caches
            List<Long> tripIds = bookingRepository.findTripIdsOfRecentlyExpired(windowStart, now);
            tripIds.forEach(tripId -> {
                String key = redisConfig.keyPrefix() + KEY_TRIP_SEATS + tripId;
                redisTemplate.delete(key);
                log.debug("Evicted seat cache for trip {}", tripId);
            });
        }
    }
}
