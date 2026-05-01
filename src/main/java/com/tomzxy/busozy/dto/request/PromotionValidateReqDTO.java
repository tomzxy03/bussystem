package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Used by FE to validate a promotion code before proceeding to payment.
 * Server validates and returns the discount amount — never computed on FE.
 */
public class PromotionValidateReqDTO {

    @NotBlank(message = "Mã khuyến mãi không được trống")
    private String promotionCode;

    @NotNull(message = "ID tuyến đường không được trống")
    private Long routeId;

    @NotNull(message = "Giá trị đơn hàng không được trống")
    @DecimalMin(value = "0.01", message = "Giá trị đơn hàng phải lớn hơn 0")
    private BigDecimal orderValue;

    private Long userId;

    public PromotionValidateReqDTO() {
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public BigDecimal getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(BigDecimal orderValue) {
        this.orderValue = orderValue;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
