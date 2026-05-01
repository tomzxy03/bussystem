package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.StopReqDTO;
import com.tomzxy.busozy.dto.response.StopResDTO;
import com.tomzxy.busozy.service.interfaces.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin – Stops", description = "Admin CRUD for bus stops")
@RestController
@RequestMapping("/api/v1/admin/stops")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class StopAdminController {

    private final LocationService locationService;

    @Operation(summary = "Tạo điểm dừng mới")
    @PostMapping
    public ResponseEntity<ApiResponse<StopResDTO>> createStop(
            @Valid @RequestBody StopReqDTO req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        StopResDTO result = locationService.createStop(req, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.created(result));
    }

    @Operation(summary = "Cập nhật điểm dừng")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StopResDTO>> updateStop(
            @PathVariable Long id,
            @Valid @RequestBody StopReqDTO req) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.updateStop(id, req), "Cập nhật thành công"));
    }

    @Operation(summary = "Xóa mềm điểm dừng")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStop(@PathVariable Long id) {
        locationService.deleteStop(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa thành công"));
    }
}
