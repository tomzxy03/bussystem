package com.tomzxy.busozy.dto.response;

import java.util.Map;

public record ReviewRatingStatsResDTO(
        Double averageRating,
        Long totalReviews,
        Map<Integer, Long> ratingDistribution) {
}
