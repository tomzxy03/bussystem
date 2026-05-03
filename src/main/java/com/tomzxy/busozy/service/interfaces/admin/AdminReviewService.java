package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.dto.response.ReviewResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminReviewService {

    Page<ReviewResDTO> getAllReviews(Pageable pageable);

    ReviewResDTO verifyReview(Long reviewId);

    void deleteReview(Long reviewId);
}
