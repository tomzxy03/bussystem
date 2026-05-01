package com.tomzxy.busozy.dto.request;

import com.tomzxy.busozy.common.enums.DiscountType;
import com.tomzxy.busozy.entity.Promotion;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class PromotionReqDTO {

    @NotBlank(message = "Mã khuyến mãi không được trống")
    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$", message = "Mã phải gồm 3-50 ký tự in hoa, số, _, -")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    private String description;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.0", message = "Giá trị giảm phải >= 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    private BigDecimal maxDiscount;

    @NotNull
    private OffsetDateTime validFrom;

    @NotNull
    private OffsetDateTime validTo;

    @Min(value = 1, message = "usage_limit phải >= 1")
    private Integer usageLimit;

    @Min(value = 1, message = "per_user_limit phải >= 1")
    private Integer perUserLimit = 1;

    private List<Long> routeIds; // empty = applies to all routes

    public PromotionReqDTO() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public List<Long> getRouteIds() {
        return routeIds;
    }

    public void setRouteIds(List<Long> routeIds) {
        this.routeIds = routeIds;
    }
}
