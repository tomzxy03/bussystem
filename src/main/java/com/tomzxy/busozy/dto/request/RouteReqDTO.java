package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class RouteReqDTO {

    @NotBlank(message = "Mã tuyến không được trống")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Mã tuyến chỉ chấp nhận chữ hoa, số, gạch dưới (3-50 ký tự)")
    private String code;

    @NotBlank(message = "Tên tuyến không được trống")
    @Size(max = 200, message = "Tên tuyến tối đa 200 ký tự")
    private String name;

    @NotNull(message = "Khoảng cách không được trống")
    @DecimalMin(value = "0.1", message = "Khoảng cách phải > 0.1 km")
    private BigDecimal distanceKm;

    @NotNull(message = "Thời gian ước tính không được trống")
    @Min(value = 1, message = "Thời gian phải >= 1 phút")
    private Integer durationMinutes;

    @NotNull(message = "ID công ty không được trống")
    private Long companyId;

    public RouteReqDTO() {
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

    public BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(BigDecimal distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
