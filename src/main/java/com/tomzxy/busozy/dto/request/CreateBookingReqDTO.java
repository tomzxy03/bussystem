package com.tomzxy.busozy.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public class CreateBookingReqDTO {

    @NotNull(message = "ID chuyến xe không được trống")
    private Long tripId;

    @NotNull(message = "Điểm đón không được trống")
    @Min(value = 1, message = "pickup_order phải >= 1")
    private Integer pickupOrder;

    @NotNull(message = "Điểm trả không được trống")
    @Min(value = 2, message = "dropoff_order phải >= 2")
    private Integer dropoffOrder;

    @NotNull
    @Size(min = 1, max = 10, message = "Số lượng hành khách từ 1 đến 10")
    private List<@Valid PassengerReqDTO> passengers;

    /** Optional promotion code — handled in Phase 9 */
    private String promotionCode;

    public CreateBookingReqDTO() {
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
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

    public List<PassengerReqDTO> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<PassengerReqDTO> passengers) {
        this.passengers = passengers;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }
}
