package com.tomzxy.busozy.dto.request;

import com.tomzxy.busozy.common.enums.StopType;
import jakarta.validation.constraints.*;

public class StopReqDTO {

    @Pattern(regexp = "^[A-Z0-9_-]{3,50}$", message = "Mã điểm dừng chỉ chấp nhận chữ hoa, số, gạch dưới, gạch ngang (3-50 ký tự)")
    private String code;

    @NotBlank(message = "Tên điểm dừng không được trống")
    @Size(min = 3, max = 100, message = "Tên phải từ 3-100 ký tự")
    private String name;

    @NotNull(message = "Loại điểm dừng không được trống")
    private StopType type;

    @NotNull(message = "Tỉnh/thành không được trống")
    private Long provinceId;

    @NotNull(message = "Quận/huyện không được trống")
    private Long districtId;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    private String address;

    @NotNull(message = "Vĩ độ không được trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ")
    private Double latitude;

    @NotNull(message = "Kinh độ không được trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ")
    private Double longitude;

    private Boolean isMajor = false;

    public StopReqDTO() {
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

    public StopType getType() {
        return type;
    }

    public void setType(StopType type) {
        this.type = type;
    }

    public Long getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Long provinceId) {
        this.provinceId = provinceId;
    }

    public Long getDistrictId() {
        return districtId;
    }

    public void setDistrictId(Long districtId) {
        this.districtId = districtId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Boolean getIsMajor() {
        return isMajor;
    }

    public void setIsMajor(Boolean isMajor) {
        this.isMajor = isMajor;
    }
}
