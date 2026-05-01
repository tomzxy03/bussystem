package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.BusStatus;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.request.BusReqDTO;
import com.tomzxy.busozy.dto.request.SeatReqDTO;
import com.tomzxy.busozy.dto.response.BusResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorBusService;
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
public class VendorBusController {

    private final VendorBusService vendorBusService;

    @Operation(summary = "Danh sách xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/buses")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Page<BusResDTO>>> searchBuses(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) BusStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(vendorBusService.searchBuses(
                currentUser, status, keyword, PageRequestFactory.build(page, size, sort, Sort.Direction.DESC))));
    }

    @Operation(summary = "Thêm xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/buses")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<BusResDTO>> createBus(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BusReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(vendorBusService.createBus(currentUser, req)));
    }

    @Operation(summary = "Cập nhật xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/buses/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<BusResDTO>> updateBus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody BusReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(vendorBusService.updateBus(currentUser, id, req), "Cập nhật thành công"));
    }

    @Operation(summary = "Đổi trạng thái xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/buses/{id}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<BusResDTO>> updateBusStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam BusStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(vendorBusService.updateBusStatus(currentUser, id, status), "Đổi trạng thái thành công"));
    }

    @Operation(summary = "Khởi tạo danh sách ghế cho xe của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/buses/{id}/seats")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<List<SeatResDTO>>> initSeats(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody List<@Valid SeatReqDTO> seats) {
        return ResponseEntity.ok(ApiResponse.ok(vendorBusService.initSeats(currentUser, id, seats)));
    }
}
