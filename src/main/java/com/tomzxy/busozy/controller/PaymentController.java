package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.dto.request.PaymentInitiateReqDTO;
import com.tomzxy.busozy.dto.response.PaymentInitiateResDTO;
import com.tomzxy.busozy.dto.response.PaymentResDTO;
import com.tomzxy.busozy.entity.PaymentMethod;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.UnauthorizedException;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.service.interfaces.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Payments", description = "Khởi tạo thanh toán, webhook, và tra cứu trạng thái")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    // ─── Public ──────────────────────────────────────────────────────────

    @Operation(summary = "Danh sách phương thức thanh toán đang hoạt động")
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<List<PaymentMethod>>> getMethods() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getActiveMethods()));
    }

    // ─── USER ────────────────────────────────────────────────────────────

    @Operation(summary = "Khởi tạo thanh toán (COD hoặc cổng online)")
    @PostMapping("/payments/initiate")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<PaymentInitiateResDTO>> initiate(
            @Valid @RequestBody PaymentInitiateReqDTO req,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                paymentService.initiatePayment(extractUserId(userDetails), req)));
    }

    @Operation(summary = "Kiểm tra trạng thái giao dịch")
    @GetMapping("/payments/{id}")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<PaymentResDTO>> getStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.getPaymentStatus(extractUserId(userDetails), id)));
    }

    @Operation(summary = "Hủy giao dịch đang chờ xử lý")
    @PostMapping("/payments/{id}/cancel")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ApiResponse<PaymentResDTO>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(
                paymentService.cancelPayment(extractUserId(userDetails), id),
                "Hủy giao dịch thành công"));
    }

    // ─── Webhook (Public — gateway calls back) ───────────────────────────

    /**
     * Receives callback from payment gateway (MoMo/VNPAY/Mock).
     * Must ALWAYS return 200 once the signature is verified and DB is updated —
     * the gateway will retry on any non-200 response.
     *
     * Note: BusinessException(PAY_004) for idempotency is caught but still returns
     * 200.
     */
    @Operation(summary = "Webhook nhận kết quả từ cổng thanh toán (Public)")
    @PostMapping("/payments/callback/{provider}")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @PathVariable String provider,
            @RequestParam Map<String, String> params) {
        try {
            paymentService.handleWebhook(provider, params);
        } catch (com.tomzxy.busozy.exception.BusinessException e) {
            if (ErrorCode.PAYMENT_ALREADY_PROCESSED.equals(e.getErrorCode())) {
                // Already processed — return 200 so gateway stops retrying
                log.debug("Webhook idempotency skip: provider={}", provider);
                return ResponseEntity.ok(ApiResponse.ok(null, "Already processed"));
            }
            throw e; // PAY_003 invalid signature — re-throw to GlobalExceptionHandler
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "OK"));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof User u)
            return u.getId();
        throw new UnauthorizedException(ErrorCode.ACCESS_DENIED);
    }
}
