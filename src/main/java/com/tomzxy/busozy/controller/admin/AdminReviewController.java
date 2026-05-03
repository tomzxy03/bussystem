package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.response.ReviewResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Reviews", description = "Admin review moderation APIs")
@RestController
@RequestMapping("/api/v1/admin/reviews")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @Operation(summary = "Danh sách review để moderation", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResDTO>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminReviewService.getAllReviews(
                        PageRequestFactory.build(page, size, "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Verify review", security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<ReviewResDTO>> verifyReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminReviewService.verifyReview(id)));
    }

    @Operation(summary = "Ẩn review spam (soft delete)", security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        adminReviewService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Ẩn đánh giá thành công"));
    }
}
