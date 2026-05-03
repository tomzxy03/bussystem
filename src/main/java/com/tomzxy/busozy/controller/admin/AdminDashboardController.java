package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.DashboardResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Dashboard", description = "Global dashboard APIs for platform admin")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @Operation(summary = "Thống kê tổng quan toàn hệ thống", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<DashboardResDTO>> getGlobalDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(adminDashboardService.getGlobalDashboard()));
    }
}
