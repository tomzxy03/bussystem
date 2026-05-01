package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVendorProfileReqDTO {

    @NotBlank(message = "Tên liên hệ không được trống")
    @Size(min = 2, max = 100, message = "Tên liên hệ phải từ 2-100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    private String email;

    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại liên hệ không hợp lệ")
    private String phone;

    @NotBlank(message = "Tên công ty không được trống")
    @Size(min = 3, max = 100, message = "Tên công ty phải từ 3-100 ký tự")
    private String companyName;

    @Pattern(regexp = "^[0-9]{10}(-[0-9]{3})?$", message = "Mã số thuế không hợp lệ (10 số hoặc 10-3 cho chi nhánh)")
    private String taxCode;

    @NotBlank(message = "Số điện thoại công ty không được trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại công ty không hợp lệ")
    private String companyPhone;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    private String address;
}
