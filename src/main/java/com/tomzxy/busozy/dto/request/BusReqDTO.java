package com.tomzxy.busozy.dto.request;

import com.tomzxy.busozy.common.enums.BusStatus;
import jakarta.validation.constraints.*;

public class BusReqDTO {

    @NotNull(message = "ID công ty không được trống")
    private Long companyId;

    @NotNull(message = "ID loại xe không được trống")
    private Long busTypeId;

    private Long seatLayoutId;

    @NotBlank(message = "Biển số xe không được trống")
    @Pattern(regexp = "^[0-9]{1,2}[A-Z]{1,2}-[0-9]{4,5}$", message = "Biển số xe không hợp lệ (ví dụ: 51B-12345)")
    private String licensePlate;

    @Size(max = 50, message = "Số xe tối đa 50 ký tự")
    private String busNumber;

    @Size(max = 100, message = "Tên xe tối đa 100 ký tự")
    private String name;

    private BusStatus status = BusStatus.ACTIVE;

    public BusReqDTO() {
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getBusTypeId() {
        return busTypeId;
    }

    public void setBusTypeId(Long busTypeId) {
        this.busTypeId = busTypeId;
    }

    public Long getSeatLayoutId() {
        return seatLayoutId;
    }

    public void setSeatLayoutId(Long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
        this.status = status;
    }
}
