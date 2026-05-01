package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.PromotionReqDTO;
import com.tomzxy.busozy.dto.request.PromotionValidateReqDTO;
import com.tomzxy.busozy.dto.response.PromotionResDTO;
import com.tomzxy.busozy.dto.response.PromotionValidateResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    /** POST /promotions/validate — 7-step validation (no side effects) */
    PromotionValidateResDTO validatePromotion(PromotionValidateReqDTO req);

    /** Called internally by BookingService on payment success */
    boolean incrementUsage(Long promotionId);
}
