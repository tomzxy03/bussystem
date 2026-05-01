package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.SeatAvailabilityDTO;
import com.tomzxy.busozy.dto.response.TripDetailResDTO;
import com.tomzxy.busozy.dto.response.TripResDTO;
import com.tomzxy.busozy.service.interfaces.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Trips", description = "Public trip search, detail, and seat availability")
@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @Operation(summary = "Tìm kiếm chuyến xe theo tuyến và ngày")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<TripResDTO>>> searchTrips(
            @RequestParam Long originStopId,
            @RequestParam Long destStopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int passengers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "departureTime,asc") String sort) {
        String[] parts = sort.split(",");
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return ResponseEntity.ok(ApiResponse.ok(
                tripService.searchTrips(originStopId, destStopId, date, passengers,
                        PageRequest.of(page, Math.min(size, 100), Sort.by(dir, parts[0])))));
    }

    @Operation(summary = "Chi tiết chuyến xe + phân đoạn + khoảng giá")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TripDetailResDTO>> getTripDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripDetail(id)));
    }

    @Operation(summary = "Sơ đồ ghế + trạng thái (for Seat Picker UI)")
    @GetMapping("/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatAvailabilityDTO>>> getTripSeats(
            @PathVariable Long id,
            @RequestParam(required = false) Integer pickupOrder,
            @RequestParam(required = false) Integer dropoffOrder) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripSeats(id, pickupOrder, dropoffOrder)));
    }
}
