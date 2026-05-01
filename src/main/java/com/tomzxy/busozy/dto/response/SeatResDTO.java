package com.tomzxy.busozy.dto.response;

import com.tomzxy.busozy.common.enums.SeatType;

import java.math.BigDecimal;

public record SeatResDTO(
        Long id,
        String seatNumber,
        SeatType seatType,
        Integer rowNum,
        Integer colNum,
        BigDecimal priceMultiplier,
        Boolean isActive) {
}
