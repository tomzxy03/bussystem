package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.OffsetTime;

public class TripCreateReqDTO {

    @NotNull(message = "ID tuyến không được trống")
    private Long routeId;

    @NotNull(message = "ID xe không được trống")
    private Long busId;

    private Long driverId;

    @NotNull(message = "Ngày khởi hành không được trống")
    @FutureOrPresent(message = "Ngày khởi hành phải từ hôm nay trở đi")
    private LocalDate departureDate;

    @NotNull(message = "Giờ khởi hành không được trống")
    private OffsetTime departureTime;

    public TripCreateReqDTO() {
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public Long getBusId() {
        return busId;
    }

    public void setBusId(Long busId) {
        this.busId = busId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public LocalDate getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(LocalDate departureDate) {
        this.departureDate = departureDate;
    }

    public OffsetTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(OffsetTime departureTime) {
        this.departureTime = departureTime;
    }
}
