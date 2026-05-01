package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public class BusTypeReqDTO {

    @NotBlank(message = "Mã loại xe không được trống")
    @Pattern(regexp = "^[A-Z0-9_]{3,30}$", message = "Mã loại xe chỉ chấp nhận chữ hoa, số, gạch dưới (3-30 ký tự)")
    private String code;

    @NotBlank(message = "Tên loại xe không được trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    private String description;

    private Map<String, Object> amenities;

    @DecimalMin(value = "0.0", message = "Giá cơ bản theo km phải >= 0")
    private BigDecimal basePricePerKm = BigDecimal.ZERO;

    public BusTypeReqDTO() {
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

    public Map<String, Object> getAmenities() {
        return amenities;
    }

    public void setAmenities(Map<String, Object> amenities) {
        this.amenities = amenities;
    }

    public BigDecimal getBasePricePerKm() {
        return basePricePerKm;
    }

    public void setBasePricePerKm(BigDecimal basePricePerKm) {
        this.basePricePerKm = basePricePerKm;
    }
}
