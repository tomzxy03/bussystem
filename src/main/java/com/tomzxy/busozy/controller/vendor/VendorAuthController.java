package com.tomzxy.busozy.controller.vendor;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.auth.RegisterVendorReqDTO;
import com.tomzxy.busozy.dto.response.AuthResDTO;
import com.tomzxy.busozy.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Vendor", description = "Vendor APIs")
@RestController
@RequestMapping("/api/v1/vendor")
@RequiredArgsConstructor
public class VendorAuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký tài khoản vendor mới")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResDTO>> registerVendor(
            @Valid @RequestBody RegisterVendorReqDTO req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthResDTO result = authService.registerVendor(req, idempotencyKey);
        return ResponseEntity.status(201).body(ApiResponse.created(result));
    }
}
