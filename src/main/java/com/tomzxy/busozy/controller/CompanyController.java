package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.CompanyResDTO;
import com.tomzxy.busozy.service.interfaces.CompanyDriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Companies", description = "Public company APIs")
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyDriverService companyDriverService;

    @Operation(summary = "Danh sách công ty đang hoạt động")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CompanyResDTO>>> getCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        int effectiveSize = Math.min(size, 100);
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return ResponseEntity.ok(ApiResponse.ok(
                companyDriverService.getCompanies(keyword,
                        PageRequest.of(page, effectiveSize, Sort.by(dir, parts[0])))));
    }

    @Operation(summary = "Chi tiết công ty")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResDTO>> getCompany(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(companyDriverService.getCompanyById(id)));
    }
}
