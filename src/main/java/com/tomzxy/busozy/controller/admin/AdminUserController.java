package com.tomzxy.busozy.controller.admin;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.annotation.Auditable;
import com.tomzxy.busozy.common.enums.UserRole;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.request.BanUserReqDTO;
import com.tomzxy.busozy.dto.response.AdminUserResDTO;
import com.tomzxy.busozy.service.interfaces.admin.AdminUserService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Users", description = "Admin user management APIs")
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Danh sách user toàn hệ thống", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserResDTO>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean banned,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminUserService.getUsers(
                        search,
                        status,
                        role,
                        banned,
                        PageRequestFactory.build(page, Math.min(size, 100), "createdAt,desc", Sort.Direction.DESC))));
    }

    @Operation(summary = "Khóa hoặc mở khóa user", security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/{id}/ban")
    @Auditable(action = "BAN_USER", entityType = "User", entityIdArgIndex = 0)
    public ResponseEntity<ApiResponse<AdminUserResDTO>> updateBanStatus(
            @PathVariable Long id,
            @Valid @RequestBody BanUserReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.updateBanStatus(id, req)));
    }
}
