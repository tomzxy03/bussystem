package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.dto.request.PromotionReqDTO;
import com.tomzxy.busozy.dto.response.PromotionResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminPromotionService {
    Page<PromotionResDTO> getAllPromotions(Pageable pageable);

    PromotionResDTO getPromotionDetail(Long id);

    PromotionResDTO createPromotion(PromotionReqDTO req);

    PromotionResDTO updatePromotion(Long id, PromotionReqDTO req);

    void deletePromotion(Long id);
}
