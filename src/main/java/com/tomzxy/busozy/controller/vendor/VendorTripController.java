package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.TripStatus;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.request.TripCreateReqDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Vendor", description = "Vendor APIs")
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorTripController {

    private final VendorTripService vendorTripService;

    @Operation(summary = "Danh sách chuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/trips")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Page<TripResDTO>>> searchTrips(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "departureDate,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(vendorTripService.searchTrips(
                currentUser, status, departureDate, PageRequestFactory.build(page, size, sort, Sort.Direction.DESC))));
    }

    @Operation(summary = "Tạo chuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/trips")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<TripResDTO>> createTrip(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TripCreateReqDTO req) {
        return ResponseEntity.status(201).body(ApiResponse.created(vendorTripService.createTrip(currentUser, req)));
    }

    @Operation(summary = "Cập nhật chuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/trips/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<TripResDTO>> updateTrip(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TripCreateReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(vendorTripService.updateTrip(currentUser, id, req), "Cập nhật thành công"));
    }

    @Operation(summary = "Đổi trạng thái chuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/trips/{id}/status")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<TripResDTO>> updateTripStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam TripStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(vendorTripService.updateTripStatus(currentUser, id, status), "Đổi trạng thái thành công"));
    }

    @Operation(summary = "Xóa mềm chuyến của vendor", security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/trips/{id}")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Void>> deleteTrip(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {
        vendorTripService.deleteTrip(currentUser, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa thành công"));
    }
}
