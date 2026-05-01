package com.tomzxy.busozy.dto.request.auth;

import jakarta.validation.constraints.*;

public class RegisterReqDTO {

    @NotBlank(message = "Tên đăng nhập không được trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3-50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Tên đăng nhập chỉ được chứa chữ, số và dấu gạch dưới")
    private String username;

    @NotBlank(message = "Email không được trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Mật khẩu không được trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 ký tự trở lên")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", message = "Mật khẩu phải chứa ít nhất 1 chữ thường, 1 chữ hoa và 1 số")
    private String password;

    @NotBlank(message = "Họ và tên không được trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2-100 ký tự")
    private String fullName;

    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ (VD: 0912345678)")
    private String phone;

    public RegisterReqDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
