package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.UpdateVendorProfileReqDTO;
import com.tomzxy.busozy.dto.response.VendorDashboardResDTO;
import com.tomzxy.busozy.dto.response.VendorProfileResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vendor", description = "Vendor APIs")
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorProfileController {

    private final VendorProfileService vendorProfileService;

    @Operation(summary = "Lấy thông tin vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me/profile")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorProfileResDTO>> getProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(vendorProfileService.getProfile(currentUser.getId())));
    }

    @Operation(summary = "Cập nhật thông tin vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/me/profile")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorProfileResDTO>> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateVendorProfileReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(vendorProfileService.updateProfile(currentUser.getId(), req), "Cập nhật thành công"));
    }

    @Operation(summary = "Lấy dashboard vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<VendorDashboardResDTO>> getDashboard(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(vendorProfileService.getDashboard(currentUser.getId())));
    }
}
