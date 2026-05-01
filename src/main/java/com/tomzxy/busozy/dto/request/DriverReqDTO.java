package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class DriverReqDTO {

    @NotNull(message = "ID công ty không được trống")
    private Long companyId;

    @NotBlank(message = "Họ và tên không được trống")
    @Size(min = 3, max = 100, message = "Họ tên phải từ 3-100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Số GPLX không được trống")
    @Pattern(regexp = "^[A-Z0-9]{6,20}$", message = "Số GPLX không hợp lệ (6-20 ký tự chữ hoa hoặc số)")
    private String licenseNumber;

    private String avatarUrl;

    private LocalDate dateOfBirth;

    @Size(max = 500)
    private String address;

    public DriverReqDTO() {
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
