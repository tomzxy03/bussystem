package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.PromotionResDTO;
import com.tomzxy.busozy.entity.Promotion;
import org.mapstruct.Mapper;

@Mapper(config = GlobalMapperConfig.class)
public interface PromotionMapper {

    PromotionResDTO toPromotionRes(Promotion p);
}
