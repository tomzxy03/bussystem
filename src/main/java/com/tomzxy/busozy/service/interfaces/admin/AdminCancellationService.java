package com.tomzxy.busozy.service.interfaces.admin;

import com.tomzxy.busozy.common.enums.RefundStatus;
import com.tomzxy.busozy.dto.response.CancellationResDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminCancellationService {

    Page<CancellationResDTO> getCancellations(String bookingCode, RefundStatus refundStatus, Pageable pageable);

    CancellationResDTO processRefund(Long cancellationId);
}
