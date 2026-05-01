package com.tomzxy.busozy.controller;

import com.tomzxy.busozy.common.ApiResponse;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.dto.request.PromotionValidateReqDTO;
import com.tomzxy.busozy.dto.response.PromotionValidateResDTO;
import com.tomzxy.busozy.entity.User;
import com.tomzxy.busozy.exception.UnauthorizedException;
import com.tomzxy.busozy.service.interfaces.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Promotion", description = "User promotion actions")
@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @Operation(summary = "Kiểm tra mã khuyến mãi và tính chiết khấu", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PromotionValidateResDTO>> validatePromotion(
            @Valid @RequestBody PromotionValidateReqDTO req,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (!(userDetails instanceof User u)) {
            throw new UnauthorizedException(ErrorCode.ACCESS_DENIED);
        }
        req.setUserId(u.getId()); // ensure the userId check matches the logged-in user

        return ResponseEntity.ok(ApiResponse.ok(promotionService.validatePromotion(req)));
    }
}
