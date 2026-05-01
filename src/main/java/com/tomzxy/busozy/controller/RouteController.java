package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import com.tomzxy.busozy.service.interfaces.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Routes", description = "Public route search and detail APIs")
@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @Operation(summary = "Tìm kiếm tuyến đường")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RouteResDTO>>> searchRoutes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long originStopId,
            @RequestParam(required = false) Long destStopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "distanceKm,asc") String sort) {
        int effectiveSize = Math.min(size, 100);
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        Page<RouteResDTO> result = routeService.searchRoutes(
                keyword, companyId, originStopId, destStopId,
                PageRequest.of(page, effectiveSize, Sort.by(dir, parts[0])));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Chi tiết tuyến + danh sách điểm dừng và giá")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RouteDetailResDTO>> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.getRouteDetail(id)));
    }

    @Operation(summary = "Bảng giá theo tuyến")
    @GetMapping("/{id}/prices")
    public ResponseEntity<ApiResponse<List<RouteDetailResDTO.RoutePriceResDTO>>> getRoutePrices(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.getRoutePrices(id)));
    }
}
