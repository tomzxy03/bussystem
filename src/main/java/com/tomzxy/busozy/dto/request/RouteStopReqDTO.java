package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RouteStopReqDTO {

    @NotNull(message = "Stop ID không được trống")
    private Long stopId;

    @NotNull(message = "Thứ tự không được trống")
    @Min(value = 1, message = "Thứ tự phải >= 1")
    private Integer stopOrder;

    @NotNull(message = "Thời gian ước tính từ điểm đầu không được trống")
    @Min(value = 0, message = "Thời gian phải >= 0")
    private Integer estimatedMinutesFromOrigin;

    @NotNull(message = "Khoảng cách từ điểm đầu không được trống")
    @DecimalMin(value = "0.0", message = "Khoảng cách phải >= 0.0")
    private BigDecimal distanceFromOrigin;

    private Boolean isPickup = true;
    private Boolean isDropoff = true;

    public RouteStopReqDTO() {
    }

    public Long getStopId() {
        return stopId;
    }

    public void setStopId(Long stopId) {
        this.stopId = stopId;
    }

    public Integer getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Integer stopOrder) {
        this.stopOrder = stopOrder;
    }

    public Integer getEstimatedMinutesFromOrigin() {
        return estimatedMinutesFromOrigin;
    }

    public void setEstimatedMinutesFromOrigin(Integer estimatedMinutesFromOrigin) {
        this.estimatedMinutesFromOrigin = estimatedMinutesFromOrigin;
    }

    public BigDecimal getDistanceFromOrigin() {
        return distanceFromOrigin;
    }

    public void setDistanceFromOrigin(BigDecimal distanceFromOrigin) {
        this.distanceFromOrigin = distanceFromOrigin;
    }

    public Boolean getIsPickup() {
        return isPickup;
    }

    public void setIsPickup(Boolean isPickup) {
        this.isPickup = isPickup;
    }

    public Boolean getIsDropoff() {
        return isDropoff;
    }

    public void setIsDropoff(Boolean isDropoff) {
        this.isDropoff = isDropoff;
    }
}
