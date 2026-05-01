package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentInitiateReqDTO {

    @NotNull(message = "ID booking không được trống")
    private Long bookingId;

    @NotBlank(message = "Phương thức thanh toán không được trống")
    private String methodCode; // COD, BANK_TRANSFER, MOMO, VNPAY

    public PaymentInitiateReqDTO() {
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getMethodCode() {
        return methodCode;
    }

    public void setMethodCode(String methodCode) {
        this.methodCode = methodCode;
    }
}
