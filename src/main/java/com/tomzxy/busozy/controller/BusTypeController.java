package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.response.BusTypeResDTO;
import com.tomzxy.busozy.dto.response.SeatResDTO;
import com.tomzxy.busozy.service.interfaces.BusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Bus Types & Seats", description = "Public APIs for bus types and seat layout")
@RestController
@RequiredArgsConstructor
public class BusTypeController {

    private final BusService busService;

    @Operation(summary = "Danh sách loại xe active")
    @GetMapping("/api/v1/bus-types")
    public ResponseEntity<ApiResponse<List<BusTypeResDTO>>> getAllBusTypes() {
        return ResponseEntity.ok(ApiResponse.ok(busService.getAllBusTypes()));
    }

    @Operation(summary = "Chi tiết loại xe")
    @GetMapping("/api/v1/bus-types/{id}")
    public ResponseEntity<ApiResponse<BusTypeResDTO>> getBusType(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(busService.getBusTypeById(id)));
    }

    @Operation(summary = "Sơ đồ ghế của xe (cho seat picker UI)")
    @GetMapping("/api/v1/buses/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatResDTO>>> getBusSeats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(busService.getBusSeats(id)));
    }
}
