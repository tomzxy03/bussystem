package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.CompanyReqDTO;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorCompanyService;
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
public class VendorCompanyController {

    private final VendorCompanyService vendorCompanyService;

    @Operation(summary = "Lấy thông tin nhà xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/company")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<CompanyResDTO>> getCompany(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(vendorCompanyService.getCompany(currentUser)));
    }

    @Operation(summary = "Cập nhật thông tin nhà xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/company")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<CompanyResDTO>> updateCompany(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CompanyReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(vendorCompanyService.updateCompany(currentUser, req), "Cập nhật thành công"));
    }
}
