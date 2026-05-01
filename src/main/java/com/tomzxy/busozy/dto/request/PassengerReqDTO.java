package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class PassengerReqDTO {

    @NotBlank(message = "Tên hành khách không được trống")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^0[35789][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    @Size(max = 20, message = "CMND/CCCD tối đa 20 ký tự")
    private String idCard;

    private LocalDate dateOfBirth;

    /**
     * Used to look up the exact Seat entity: seat_number = seatRow + seatCol (e.g.,
     * "1A").
     * Server resolves to real seat_id — never trusted from FE directly.
     */
    @NotNull(message = "Hàng ghế không được trống")
    private Integer seatRow;

    @NotBlank(message = "Cột ghế không được trống")
    private String seatCol;

    public PassengerReqDTO() {
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

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(Integer seatRow) {
        this.seatRow = seatRow;
    }

    public String getSeatCol() {
        return seatCol;
    }

    public void setSeatCol(String seatCol) {
        this.seatCol = seatCol;
    }

    /**
     * Derives the canonical seat_number stored in DB (e.g., row=1, col="A" → "1A")
     */
    public String toSeatNumber() {
        return seatRow + seatCol.toUpperCase();
    }
}
