package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.ReviewRatingStatsResDTO;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import com.tomzxy.busozy.service.interfaces.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trip Reviews", description = "Public trip reviews")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Lấy reviews của chuyến xe")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResDTO>>> getTripReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getTripReviews(id,
                        PageRequestFactory.build(page, size, "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Thống kê rating của chuyến xe")
    @GetMapping("/{id}/rating-stats")
    public ResponseEntity<ApiResponse<ReviewRatingStatsResDTO>> getTripRatingStats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getTripRatingStats(id)));
    }
}
