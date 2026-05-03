package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.RefundStatus;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.response.CancellationResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminCancellationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Cancellations", description = "Admin cancellation management APIs")
@RestController
@RequestMapping("/api/v1/admin/cancellations")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminCancellationController {

    private final AdminCancellationService adminCancellationService;

    @Operation(summary = "Danh sách hủy vé", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CancellationResDTO>>> getCancellations(
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) RefundStatus refundStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCancellationService.getCancellations(
                        bookingCode,
                        refundStatus,
                        PageRequestFactory.build(page, Math.min(size, 100), "cancelTime,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Xử lý lại hoàn tiền", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{id}/process-refund")
    public ResponseEntity<ApiResponse<CancellationResDTO>> processRefund(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCancellationService.processRefund(id),
                "Đã xử lý yêu cầu hoàn tiền"));
    }
}
