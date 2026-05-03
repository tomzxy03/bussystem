package com.tomzxy.busozy.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingReqDTO {

    @NotBlank
    @Size(max = 500)
    private String reason;

    @AssertTrue(message = "Bạn phải xác nhận đã đọc chính sách hoàn tiền")
    private Boolean confirmRefundPolicy;
}
