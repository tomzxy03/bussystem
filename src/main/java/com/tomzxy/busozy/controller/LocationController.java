package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.StopType;
import com.tomzxy.busozy.dto.response.DistrictResDTO;
import com.tomzxy.busozy.dto.response.ProvinceResDTO;
import com.tomzxy.busozy.dto.response.StopResDTO;
import com.tomzxy.busozy.service.interfaces.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Location", description = "Public location APIs (provinces, districts, stops)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @Operation(summary = "Danh sách tỉnh/thành phố")
    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResDTO>>> getProvinces(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getProvinces(keyword)));
    }

    @Operation(summary = "Danh sách quận/huyện theo tỉnh")
    @GetMapping("/provinces/{id}/districts")
    public ResponseEntity<ApiResponse<List<DistrictResDTO>>> getDistricts(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getDistrictsByProvince(id)));
    }

    @Operation(summary = "Tìm kiếm điểm dừng")
    @GetMapping("/stops")
    public ResponseEntity<ApiResponse<Page<StopResDTO>>> searchStops(
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) StopType type,
            @RequestParam(required = false) Boolean isMajor,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        // Cap page size at 100 as per spec
        int effectiveSize = Math.min(size, 100);
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, effectiveSize, Sort.by(direction, sortField));

        Page<StopResDTO> result = locationService.searchStops(
                provinceId, districtId, type, isMajor, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Chi tiết điểm dừng")
    @GetMapping("/stops/{id}")
    public ResponseEntity<ApiResponse<StopResDTO>> getStop(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getStopById(id)));
    }
}
