package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.response.ActivityLogResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Logs", description = "Audit log APIs for platform admin")
@RestController
@RequestMapping("/api/v1/admin/logs")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminActivityLogController {

    private final AdminActivityLogService adminActivityLogService;

    @Operation(summary = "Xem nhật ký hoạt động", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ActivityLogResDTO>>> getLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminActivityLogService.getLogs(
                        action,
                        entityType,
                        PageRequestFactory.build(page, Math.min(size, 100), "createdAt,desc", Sort.Direction.DESC))));
    }
}
