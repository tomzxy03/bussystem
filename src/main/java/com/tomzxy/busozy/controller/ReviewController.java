package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.ReviewReqDTO;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import com.tomzxy.busozy.service.interfaces.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews", description = "Review APIs for customers")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Tạo review cho chuyến đã hoàn thành")
    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResDTO>> createReview(
            @Valid @RequestBody ReviewReqDTO req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                reviewService.createReview(extractUserId(userDetails), req)));
    }

    @Operation(summary = "Danh sách review của tôi")
    @GetMapping("/users/me/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResDTO>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getMyReviews(
                        extractUserId(userDetails),
                        PageRequestFactory.build(page, size, "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Xóa review của chính mình")
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.deleteOwnReview(extractUserId(userDetails), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa đánh giá thành công"));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.tomzxy.busozy.entity.User user) {
            return user.getId();
        }
        throw new com.tomzxy.busozy.exception.UnauthorizedException(
                com.tomzxy.busozy.common.enums.ErrorCode.ACCESS_DENIED);
    }
}
