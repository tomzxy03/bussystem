package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class TripSearchReqDTO {

    @NotNull(message = "Điểm đón không được trống")
    private Long originStopId;

    @NotNull(message = "Điểm trả không được trống")
    private Long destStopId;

    @NotNull(message = "Ngày khởi hành không được trống")
    private LocalDate date;

    @Min(value = 1, message = "Số hành khách phải >= 1")
    private Integer passengers = 1;

    public TripSearchReqDTO() {
    }

    public Long getOriginStopId() {
        return originStopId;
    }

    public void setOriginStopId(Long originStopId) {
        this.originStopId = originStopId;
    }

    public Long getDestStopId() {
        return destStopId;
    }

    public void setDestStopId(Long destStopId) {
        this.destStopId = destStopId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPassengers() {
        return passengers;
    }

    public void setPassengers(Integer passengers) {
        this.passengers = passengers;
    }
}
