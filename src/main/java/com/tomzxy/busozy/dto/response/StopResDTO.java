package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.StopType;

public record StopResDTO(
        Long id,
        String code,
        String name,
        StopType type,
        String provinceName,
        String districtName,
        String address,
        Double lat,
        Double lng,
        Boolean isMajor) {
}
