package com.tomzxy.busozy.dto.request.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVendorReqDTO {

    @Valid
    @NotNull(message = "Thông tin tài khoản không được trống")
    private RegisterReqDTO account;

    @NotBlank(message = "Tên nhà xe không được trống")
    @Size(min = 3, max = 100, message = "Tên nhà xe phải từ 3-100 ký tự")
    private String companyName;

    @Pattern(regexp = "^[0-9]{10}(-[0-9]{3})?$", message = "Mã số thuế không hợp lệ (10 số hoặc 10-3 cho chi nhánh)")
    private String taxCode;

    @NotBlank(message = "Số điện thoại công ty không được trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String companyPhone;

    @Size(max = 500, message = "Địa chỉ không quá 500 ký tự")
    private String address;
}
