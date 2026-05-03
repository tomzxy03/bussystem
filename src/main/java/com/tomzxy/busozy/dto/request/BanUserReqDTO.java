package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BanUserReqDTO(
        @NotNull(message = "Trạng thái banned là bắt buộc")
        Boolean banned,
        @Size(max = 255, message = "Lý do khóa tài khoản tối đa 255 ký tự")
        String reason) {
}
