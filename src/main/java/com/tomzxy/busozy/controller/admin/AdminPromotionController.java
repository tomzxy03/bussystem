package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.request.PromotionReqDTO;
import com.tomzxy.busozy.dto.response.PromotionResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminPromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Promotion", description = "Admin promotion management APIs")
@RestController
@RequestMapping("/api/v1/admin/promotions")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    @Operation(summary = "Lấy danh sách khuyến mãi (Admin/Staff)", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PromotionResDTO>>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminPromotionService.getAllPromotions(
                        PageRequestFactory.build(page, Math.min(size, 100), "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Xem chi tiết khuyến mãi", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResDTO>> getPromotionDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminPromotionService.getPromotionDetail(id)));
    }

    @Operation(summary = "Tạo mới khuyến mãi", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<PromotionResDTO>> createPromotion(@Valid @RequestBody PromotionReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(adminPromotionService.createPromotion(req)));
    }

    @Operation(summary = "Cập nhật khuyến mãi", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionResDTO>> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(adminPromotionService.updatePromotion(id, req)));
    }

    @Operation(summary = "Xoá mềm khuyến mãi", security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(@PathVariable Long id) {
        adminPromotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
