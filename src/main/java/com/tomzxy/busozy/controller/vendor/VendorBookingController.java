package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.controller.PageRequestFactory;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.vendor.VendorBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vendor", description = "Vendor booking management")
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorBookingController {

    private final VendorBookingService vendorBookingService;

    @Operation(summary = "Danh sách đơn đặt vé của nhà xe", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/bookings")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<ApiResponse<Page<BookingResDTO>>> getBookings(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) com.tomzxy.busozy.common.enums.BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                vendorBookingService.getVendorBookings(currentUser, status,
                        PageRequestFactory.build(page, Math.min(size, 100), "createdAt,desc", Sort.Direction.DESC))));
    }
}
