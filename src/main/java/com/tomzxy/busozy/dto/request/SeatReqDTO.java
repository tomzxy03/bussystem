package com.tomzxy.busozy.dto.request;

import com.tomzxy.busozy.common.enums.SeatType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class SeatReqDTO {

    @NotBlank(message = "Mã ghế không được trống")
    @Pattern(regexp = "^[A-Z0-9]{1,4}$", message = "Mã ghế không hợp lệ (1-4 ký tự chữ hoa hoặc số)")
    private String seatNumber;

    private SeatType seatType = SeatType.STANDARD;

    private Integer rowNum;
    private Integer colNum;

    @DecimalMin(value = "0.1", message = "Hệ số giá tối thiểu là 0.1")
    @DecimalMax(value = "5.0", message = "Hệ số giá tối đa là 5.0")
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    public SeatReqDTO() {
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public void setSeatType(SeatType seatType) {
        this.seatType = seatType;
    }

    public Integer getRowNum() {
        return rowNum;
    }

    public void setRowNum(Integer rowNum) {
        this.rowNum = rowNum;
    }

    public Integer getColNum() {
        return colNum;
    }

    public void setColNum(Integer colNum) {
        this.colNum = colNum;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public void setPriceMultiplier(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }
}
