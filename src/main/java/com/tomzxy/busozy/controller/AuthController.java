package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.LoginReqDTO;
import com.tomzxy.busozy.dto.request.UpdateProfileReqDTO;
import com.tomzxy.busozy.dto.request.auth.RegisterReqDTO;
import com.tomzxy.busozy.dto.response.AuthResDTO;
import com.tomzxy.busozy.dto.response.UserResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.service.interfaces.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Authentication & Authorization APIs")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký tài khoản mới")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResDTO>> register(
            @Valid @RequestBody RegisterReqDTO req,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthResDTO result = authService.register(req, idempotencyKey);
        return ResponseEntity.status(201)
                .body(ApiResponse.created(result));
    }

    
    @Operation(summary = "Đăng nhập")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResDTO>> login(
            @Valid @RequestBody LoginReqDTO req,
            HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        AuthResDTO result = authService.login(req, clientIp);
        return ResponseEntity.ok(ApiResponse.ok(result, "Đăng nhập thành công"));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResDTO>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        AuthResDTO result = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Đăng xuất", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // strip "Bearer "
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Đăng xuất thành công"));
    }

    @Operation(summary = "Lấy thông tin cá nhân", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResDTO>> getMe(
            @AuthenticationPrincipal User currentUser) {
        UserResDTO result = authService.getMyProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Cập nhật thông tin cá nhân", security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResDTO>> updateMe(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileReqDTO req) {
        UserResDTO result = authService.updateMyProfile(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok(result, "Cập nhật thành công"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
