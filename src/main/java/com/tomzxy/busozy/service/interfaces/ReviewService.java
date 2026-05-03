package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.ReviewReqDTO;
import com.tomzxy.busozy.dto.response.ReviewRatingStatsResDTO;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResDTO createReview(Long userId, ReviewReqDTO req);

    Page<ReviewResDTO> getTripReviews(Long tripId, Pageable pageable);

    ReviewRatingStatsResDTO getTripRatingStats(Long tripId);

    Page<ReviewResDTO> getMyReviews(Long userId, Pageable pageable);

    void deleteOwnReview(Long userId, Long reviewId);

    ReviewResDTO verifyReview(Long reviewId);
}
