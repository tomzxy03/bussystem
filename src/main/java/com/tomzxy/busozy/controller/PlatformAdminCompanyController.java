package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.CompanyReqDTO;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.service.interfaces.CompanyDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Platform Admin – Companies", description = "Platform admin APIs for managing transport companies")
@RestController
@RequestMapping("/api/v1/admin/companies")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformAdminCompanyController {

    private final CompanyDriverService companyDriverService;

    @Operation(summary = "Tạo công ty mới")
    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResDTO>> createCompany(@Valid @RequestBody CompanyReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(companyDriverService.createCompany(req)));
    }

    @Operation(summary = "Cập nhật công ty")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResDTO>> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(companyDriverService.updateCompany(id, req), "Cập nhật thành công"));
    }

    @Operation(summary = "Xóa mềm công ty")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompany(@PathVariable Long id) {
        companyDriverService.deleteCompany(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa thành công"));
    }
}
