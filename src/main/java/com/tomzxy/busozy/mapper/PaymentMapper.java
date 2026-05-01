package com.tomzxy.busozy.mapper;

import com.tomzxy.busozy.config.GlobalMapperConfig;
import com.tomzxy.busozy.dto.response.PaymentResDTO;
import com.tomzxy.busozy.entity.Payment;
import com.tomzxy.busozy.entity.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "id")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "methodCode", expression = "java(mapMethodCode(p.getPaymentMethod()))")
    PaymentResDTO toStatusRes(Payment p);

    default String mapMethodCode(PaymentMethod pm) {
        return pm != null ? pm.getCode() : null;
    }
}
