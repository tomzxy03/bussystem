package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RoutePriceReqDTO {

    @NotNull(message = "Điểm đón không được trống")
    @Min(value = 1, message = "pickup_order phải >= 1")
    private Integer pickupOrder;

    @NotNull(message = "Điểm trả không được trống")
    @Min(value = 2, message = "dropoff_order phải >= 2")
    private Integer dropoffOrder;

    @NotNull(message = "Giá vé không được trống")
    @DecimalMin(value = "1000", message = "Giá vé tối thiểu 1.000 VND")
    private BigDecimal price;

    private String currency = "VND";

    public RoutePriceReqDTO() {
    }

    public Integer getPickupOrder() {
        return pickupOrder;
    }

    public void setPickupOrder(Integer pickupOrder) {
        this.pickupOrder = pickupOrder;
    }

    public Integer getDropoffOrder() {
        return dropoffOrder;
    }

    public void setDropoffOrder(Integer dropoffOrder) {
        this.dropoffOrder = dropoffOrder;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
