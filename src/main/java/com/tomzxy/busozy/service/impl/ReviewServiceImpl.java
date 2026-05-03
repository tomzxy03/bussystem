package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.request.ReviewReqDTO;
import com.tomzxy.busozy.dto.response.ReviewRatingStatsResDTO;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.entity.Review;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ConflictException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.ReviewNotificationMapper;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.repository.ReviewRepository;
import com.tomzxy.busozy.repository.TripRepository;
import com.tomzxy.busozy.repository.UserRepository;
import com.tomzxy.busozy.service.interfaces.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final Duration RATING_STATS_TTL = Duration.ofMinutes(10);
    private static final String KEY_TRIP_RATING_STATS = "trip:rating-stats:";

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ReviewNotificationMapper mapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    @Override
    @Transactional
    public ReviewResDTO createReview(Long userId, ReviewReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_ACCESS_DENIED);
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_COMPLETED);
        }
        if (reviewRepository.existsAnyByBookingId(booking.getId())) {
            throw new ConflictException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = new Review();
        review.setUser(user);
        review.setTrip(booking.getTrip());
        review.setBooking(booking);
        review.setRating(req.getRating());
        review.setComment(sanitizeComment(req.getComment()));
        review.setIsVerified(false);

        reviewRepository.save(review);
        tripRepository.findById(booking.getTrip().getId()).orElseThrow(() ->
                new ResourceNotFoundException(ErrorCode.TRIP_NOT_FOUND));
        evictTripRatingStats(booking.getTrip().getId());
        return mapper.toReviewRes(review);
    }

    @Override
    public Page<ReviewResDTO> getTripReviews(Long tripId, Pageable pageable) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException(ErrorCode.TRIP_NOT_FOUND);
        }
        return reviewRepository.findByTripIdOrderByCreatedAtDesc(tripId, pageable)
                .map(mapper::toPublicReviewRes);
    }

    @Override
    public ReviewRatingStatsResDTO getTripRatingStats(Long tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResourceNotFoundException(ErrorCode.TRIP_NOT_FOUND);
        }

        String cacheKey = tripRatingStatsCacheKey(tripId);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ReviewRatingStatsResDTO stats) {
            return stats;
        }

        ReviewRepository.ReviewRatingSummaryProjection summary = reviewRepository.getRatingSummaryByTripId(tripId);
        Map<Integer, Long> ratingDistribution = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) {
            ratingDistribution.put(rating, 0L);
        }
        for (ReviewRepository.ReviewRatingCountProjection count : reviewRepository.countRatingsByTripId(tripId)) {
            ratingDistribution.put(count.getRating(), count.getTotal());
        }

        double averageRating = summary != null && summary.getAverageRating() != null
                ? BigDecimal.valueOf(summary.getAverageRating()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0d;
        long totalReviews = summary != null && summary.getTotalReviews() != null ? summary.getTotalReviews() : 0L;

        ReviewRatingStatsResDTO stats = new ReviewRatingStatsResDTO(averageRating, totalReviews, ratingDistribution);
        redisTemplate.opsForValue().set(cacheKey, stats, RATING_STATS_TTL);
        return stats;
    }

    @Override
    public Page<ReviewResDTO> getMyReviews(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
        return reviewRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(mapper::toReviewRes);
    }

    @Override
    @Transactional
    public void deleteOwnReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REVIEW_NOT_FOUND));
        review.setDeletedAt(OffsetDateTime.now());
        reviewRepository.save(review);
        evictTripRatingStats(review.getTrip().getId());
    }

    @Override
    @Transactional
    public ReviewResDTO verifyReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REVIEW_NOT_FOUND));
        review.setIsVerified(true);
        reviewRepository.save(review);
        evictTripRatingStats(review.getTrip().getId());
        return mapper.toReviewRes(review);
    }

    private String sanitizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.replaceAll("<[^>]*>", "").trim();
    }

    private String tripRatingStatsCacheKey(Long tripId) {
        return redisConfig.keyPrefix() + KEY_TRIP_RATING_STATS + tripId;
    }

    private void evictTripRatingStats(Long tripId) {
        redisTemplate.delete(tripRatingStatsCacheKey(tripId));
    }
}
