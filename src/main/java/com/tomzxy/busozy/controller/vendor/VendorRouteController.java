package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.request.RoutePriceReqDTO;
import com.tomzxy.busozy.dto.request.RouteReqDTO;
import com.tomzxy.busozy.dto.request.RouteStopReqDTO;
import com.tomzxy.busozy.dto.response.RouteDetailResDTO;
import com.tomzxy.busozy.dto.response.RouteResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class VendorRouteController {

    private final VendorRouteService vendorRouteService;

    @Operation(summary = "Danh sách tuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/routes")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Page<RouteResDTO>>> searchRoutes(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(vendorRouteService.searchRoutes(
                currentUser, keyword, PageRequestFactory.build(page, size, sort, Sort.Direction.ASC))));
    }

    @Operation(summary = "Tạo tuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/routes")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<RouteResDTO>> createRoute(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RouteReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(vendorRouteService.createRoute(currentUser, req)));
    }

    @Operation(summary = "Cập nhật tuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/routes/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<RouteResDTO>> updateRoute(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody RouteReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(vendorRouteService.updateRoute(currentUser, id, req), "Cập nhật thành công"));
    }

    @Operation(summary = "Cập nhật điểm dừng tuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/routes/{id}/stops")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<RouteDetailResDTO>> replaceStops(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody List<@Valid RouteStopReqDTO> stops) {
        return ResponseEntity.ok(ApiResponse.ok(vendorRouteService.replaceRouteStops(currentUser, id, stops)));
    }

    @Operation(summary = "Cập nhật bảng giá tuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/routes/{id}/prices")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<RouteDetailResDTO>> replacePrices(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody List<@Valid RoutePriceReqDTO> prices) {
        return ResponseEntity.ok(ApiResponse.ok(vendorRouteService.replaceRoutePrices(currentUser, id, prices)));
    }

    @Operation(summary = "Xóa mềm tuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/routes/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        vendorRouteService.deleteRoute(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa thành công"));
    }
}
