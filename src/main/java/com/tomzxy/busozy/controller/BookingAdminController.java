package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.service.interfaces.admin.AdminBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin – Bookings", description = "Admin view of all bookings")
@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class BookingAdminController {

    private final AdminBookingService adminBookingService;

    @Operation(summary = "Danh sách tất cả bookings (có lọc status)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResDTO>>> getAllBookings(
            @RequestParam(required = false) com.tomzxy.busozy.common.enums.BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminBookingService.getAdminBookings(status,
                        PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")))));
    }
}
