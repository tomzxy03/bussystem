package com.tomzxy.busozy.dto.request;

import com.tomzxy.busozy.common.enums.Gender;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class UpdateProfileReqDTO {

    @NotBlank(message = "Họ và tên không được trống")
    private String fullName;

    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dateOfBirth;

    @NotNull(message = "Giới tính không được trống")
    private Gender gender;

    private String address;

    private String avatarUrl;

    public UpdateProfileReqDTO() {
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

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
