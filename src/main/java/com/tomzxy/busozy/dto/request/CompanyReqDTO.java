package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

public class CompanyReqDTO {

    @NotBlank(message = "Tên công ty không được trống")
    @Size(min = 3, max = 100, message = "Tên phải từ 3-100 ký tự")
    private String name;

    @Pattern(regexp = "^[0-9]{10}(-[0-9]{3})?$", message = "Mã số thuế không hợp lệ (10 số hoặc 10-3 cho chi nhánh)")
    private String taxCode;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    private String address;

    public CompanyReqDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
