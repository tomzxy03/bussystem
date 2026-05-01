package com.tomzxy.busozy.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        int code,
        OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "Thành công", null, 200, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, 200, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, data, "Tạo thành công", null, 201, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message, int status) {
        return new ApiResponse<>(false, null, message, errorCode, status, OffsetDateTime.now());
    }
}
