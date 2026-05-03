package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.config.RedisConfig;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import com.tomzxy.busozy.entity.Review;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.service.interfaces.ReviewService;
import com.tomzxy.busozy.service.interfaces.admin.AdminReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private static final String KEY_TRIP_RATING_STATS = "trip:rating-stats:";

    private final com.tomzxy.busozy.repository.ReviewRepository reviewRepository;
    private final com.tomzxy.busozy.mapper.ReviewNotificationMapper mapper;
    private final ReviewService reviewService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    @Override
    public Page<ReviewResDTO> getAllReviews(Pageable pageable) {
        return reviewRepository.findAllForAdmin(pageable).map(mapper::toReviewRes);
    }

    @Override
    public ReviewResDTO verifyReview(Long reviewId) {
        return reviewService.verifyReview(reviewId);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.REVIEW_NOT_FOUND));
        review.setIsVerified(false);
        review.setDeletedAt(OffsetDateTime.now());
        reviewRepository.save(review);
        redisTemplate.delete(redisConfig.keyPrefix() + KEY_TRIP_RATING_STATS + review.getTrip().getId());
    }
}
