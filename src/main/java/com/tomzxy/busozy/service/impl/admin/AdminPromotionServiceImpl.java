package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.dto.request.PromotionReqDTO;
import com.tomzxy.busozy.dto.response.PromotionResDTO;
import com.tomzxy.busozy.entity.Promotion;
import com.tomzxy.busozy.entity.Route;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.PromotionMapper;
import com.tomzxy.busozy.repository.PromotionRepository;
import com.tomzxy.busozy.repository.RouteRepository;
import com.tomzxy.busozy.service.interfaces.admin.AdminPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPromotionServiceImpl implements AdminPromotionService {

    private final PromotionRepository promotionRepository;
    private final RouteRepository routeRepository;
    private final PromotionMapper promotionMapper;

    @Override
    public Page<PromotionResDTO> getAllPromotions(Pageable pageable) {
        return promotionRepository.findAllByOrderByCreatedAtDesc(pageable).map(promotionMapper::toPromotionRes);
    }

    @Override
    public PromotionResDTO getPromotionDetail(Long id) {
        return promotionRepository.findById(id)
                .map(promotionMapper::toPromotionRes)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROMOTION_NOT_FOUND));
    }

    @Override
    @Transactional
    public PromotionResDTO createPromotion(PromotionReqDTO req) {
        Promotion promo = new Promotion();
        updatePromotionFromReq(promo, req);
        promotionRepository.save(promo);
        return promotionMapper.toPromotionRes(promo);
    }

    @Override
    @Transactional
    public PromotionResDTO updatePromotion(Long id, PromotionReqDTO req) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROMOTION_NOT_FOUND));
        updatePromotionFromReq(promo, req);
        promotionRepository.save(promo);
        return promotionMapper.toPromotionRes(promo);
    }

    @Override
    @Transactional
    public void deletePromotion(Long id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.PROMOTION_NOT_FOUND));
        promo.setIsActive(false);
        promotionRepository.delete(promo);
    }

    private void updatePromotionFromReq(Promotion promo, PromotionReqDTO req) {
        promo.setCode(req.getCode().toUpperCase());
        promo.setName(req.getName());
        promo.setDescription(req.getDescription());
        promo.setDiscountType(req.getDiscountType());
        promo.setDiscountValue(req.getDiscountValue());
        promo.setMinOrderValue(req.getMinOrderValue() == null ? BigDecimal.ZERO : req.getMinOrderValue());
        promo.setMaxDiscount(req.getMaxDiscount());
        promo.setValidFrom(req.getValidFrom());
        promo.setValidTo(req.getValidTo());
        promo.setUsageLimit(req.getUsageLimit());
        promo.setPerUserLimit(req.getPerUserLimit() == null ? 1 : req.getPerUserLimit());

        // Resolve routes
        if (req.getRouteIds() != null && !req.getRouteIds().isEmpty()) {
            List<Route> routes = routeRepository.findAllById(req.getRouteIds());
            if (routes.size() != req.getRouteIds().size()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Một hoặc nhiều tuyến đường không tồn tại");
            }
            promo.setApplicableRoutes(new HashSet<>(routes));
        } else {
            promo.getApplicableRoutes().clear();
        }
    }
}
