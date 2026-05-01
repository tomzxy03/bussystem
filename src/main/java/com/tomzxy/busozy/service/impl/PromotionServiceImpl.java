package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.DiscountType;
import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.dto.request.PromotionValidateReqDTO;
import com.tomzxy.busozy.dto.response.PromotionValidateResDTO;
import com.tomzxy.busozy.entity.Promotion;
import com.tomzxy.busozy.repository.PromotionRepository;
import com.tomzxy.busozy.repository.RouteRepository;
import com.tomzxy.busozy.repository.UserPromotionUsageRepository;
import com.tomzxy.busozy.service.interfaces.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;

    @Override
    public PromotionValidateResDTO validatePromotion(PromotionValidateReqDTO req) {
        // 1. Find active promotion
        Promotion promo = promotionRepository.findByCodeActiveIgnoreCase(req.getPromotionCode())
                .orElse(null);
        if (promo == null) {
            return new PromotionValidateResDTO(false, null, null, null, null,
                    "Mã khuyến mãi không tồn tại hoặc không hoạt động");
        }

        // 2. Check validity window with strict Vietnam Timezone enforcement
        java.time.ZoneId vnZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        OffsetDateTime nowVn = OffsetDateTime.now(vnZone);
        if (nowVn.isBefore(promo.getValidFrom()) || nowVn.isAfter(promo.getValidTo())) {
            return new PromotionValidateResDTO(false, null, null, null, null, "Mã khuyến mãi không còn hiệu lực");
        }

        // 3. Check global usage limit
        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return new PromotionValidateResDTO(false, null, null, null, null, "Mã khuyến mãi đã hết lượt sử dụng");
        }

        // 4. Check per-user limit
        if (promo.getPerUserLimit() != null) {
            int userUsed = userPromotionUsageRepository.countByUserIdAndPromotionId(req.getUserId(), promo.getId());
            if (userUsed >= promo.getPerUserLimit()) {
                return new PromotionValidateResDTO(false, null, null, null, null,
                        "Bạn đã dùng mã này tối đa số lần cho phép");
            }
        }

        // 5. Check min order value
        if (req.getOrderValue().compareTo(promo.getMinOrderValue()) < 0) {
            return new PromotionValidateResDTO(false, null, null, null, null,
                    "Đơn hàng tối thiểu " + promo.getMinOrderValue() + " VND để áp dụng mã");
        }

        // 6. Check route restriction optimally without loading the whole Set
        if (!promo.getApplicableRoutes().isEmpty()) {
            boolean routeMatch = promotionRepository.existsByPromotionIdAndRouteId(promo.getId(), req.getRouteId());
            if (!routeMatch) {
                return new PromotionValidateResDTO(false, null, null, null, null,
                        "Mã không áp dụng cho tuyến đường này");
            }
        }

        // 7. Calculate discount
        BigDecimal discountAmount = calculateDiscount(promo, req.getOrderValue());
        BigDecimal finalPrice = req.getOrderValue().subtract(discountAmount);
        // Fallback max check inside calculation logic protects against negative prices,
        // but good to be explicit:
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0)
            finalPrice = BigDecimal.ZERO;

        return new PromotionValidateResDTO(true, promo.getCode(), promo.getName(),
                discountAmount, finalPrice, "Áp dụng thành công");
    }

    private BigDecimal calculateDiscount(Promotion promo, BigDecimal orderValue) {
        BigDecimal discount;
        if (promo.getDiscountType().equals(DiscountType.PERCENTAGE)) {
            discount = orderValue
                    .multiply(promo.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
                discount = promo.getMaxDiscount();
            }
        } else {
            // FIXED
            discount = promo.getDiscountValue();
            if (discount.compareTo(orderValue) > 0) {
                discount = orderValue; // Không giảm quá giá trị đơn
            }
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public boolean incrementUsage(Long promotionId) {
        int updated = promotionRepository.incrementUsedCount(promotionId);
        return updated > 0;
    }
}
