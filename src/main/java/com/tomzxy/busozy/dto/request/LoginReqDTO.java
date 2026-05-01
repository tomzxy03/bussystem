package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LoginReqDTO {

    @NotBlank(message = "Tên đăng nhập hoặc email không được trống")
    private String credential;

    @NotBlank(message = "Mật khẩu không được trống")
    private String password;

    public LoginReqDTO() {
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
