package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.CreateBookingReqDTO;
import com.tomzxy.busozy.dto.response.BookingDetailResDTO;
import com.tomzxy.busozy.dto.response.BookingResDTO;
import com.tomzxy.busozy.service.interfaces.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Bookings", description = "Đặt vé, tra cứu, và hủy vé")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @Operation(summary = "Tạo booking mới (giữ ghế 10 phút)")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResDTO>> createBooking(
            @Valid @RequestBody CreateBookingReqDTO req,
            @RequestHeader(value = "Idempotency-Key", required = false, defaultValue = "") String idempotencyKey,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        // Use a stable key: if blank, generate per-request (idempotency won't fire but
        // still safe)
        String key = idempotencyKey.isBlank() ? UUID.randomUUID().toString() : idempotencyKey;
        return ResponseEntity.status(201).body(ApiResponse.created(
                bookingService.createBooking(userId, req, key)));
    }

    @Operation(summary = "Lịch sử đặt vé của tôi")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<BookingResDTO>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getMyBookings(userId,
                        PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @Operation(summary = "Chi tiết booking theo booking_code")
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<BookingDetailResDTO>> getBookingDetail(
            @PathVariable UUID code,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.getBookingDetail(extractUserId(userDetails), code)));
    }

    @Operation(summary = "Hủy booking")
    @PostMapping("/{code}/cancel")
    public ResponseEntity<ApiResponse<BookingResDTO>> cancelBooking(
            @PathVariable UUID code,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingService.cancelBooking(extractUserId(userDetails), code),
                "Hủy vé thành công"));
    }

    /**
     * Safely extracts numeric userId from JWT-authenticated UserDetails principal.
     */
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.tomzxy.busozy.entity.User u) {
            return u.getId();
        }
        throw new com.tomzxy.busozy.exception.UnauthorizedException(
                com.tomzxy.busozy.common.enums.ErrorCode.ACCESS_DENIED);
    }
}
