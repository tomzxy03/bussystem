package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.request.CancellationPolicyReqDTO;
import com.tomzxy.busozy.dto.response.CancellationPolicyResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminCancellationPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Cancellation Policies", description = "Admin cancellation policy management APIs")
@RestController
@RequestMapping("/api/v1/admin/cancellation-policies")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminCancellationPolicyController {

    private final AdminCancellationPolicyService adminCancellationPolicyService;

    @Operation(summary = "Danh sách policy hoàn tiền", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CancellationPolicyResDTO>>> getPolicies(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCancellationPolicyService.getPolicies(
                        companyId,
                        routeId,
                        active,
                        PageRequestFactory.build(page, Math.min(size, 100), "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Tạo policy hoàn tiền", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping
    public ResponseEntity<ApiResponse<CancellationPolicyResDTO>> createPolicy(@Valid @RequestBody CancellationPolicyReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(adminCancellationPolicyService.createPolicy(req)));
    }

    @Operation(summary = "Cập nhật policy hoàn tiền", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CancellationPolicyResDTO>> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody CancellationPolicyReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(adminCancellationPolicyService.updatePolicy(id, req)));
    }
}
