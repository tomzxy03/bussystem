package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.UserStatus;
import com.tomzxy.busozy.dto.request.DriverReqDTO;
import com.tomzxy.busozy.dto.response.DriverResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Vendor", description = "Vendor APIs")
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorDriverController {

    private final VendorDriverService vendorDriverService;

    @Operation(summary = "Danh sách tài xế của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/drivers")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<List<DriverResDTO>>> getDrivers(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(vendorDriverService.getDrivers(currentUser)));
    }

    @Operation(summary = "Thêm tài xế của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/drivers")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<DriverResDTO>> createDriver(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DriverReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(vendorDriverService.createDriver(currentUser, req)));
    }

    @Operation(summary = "Cập nhật tài xế của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/drivers/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<DriverResDTO>> updateDriver(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody DriverReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(vendorDriverService.updateDriver(currentUser, id, req), "Cập nhật thành công"));
    }

    @Operation(summary = "Đổi trạng thái tài xế của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/drivers/{id}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<DriverResDTO>> updateDriverStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(vendorDriverService.updateDriverStatus(currentUser, id, status), "Đổi trạng thái thành công"));
    }
}
