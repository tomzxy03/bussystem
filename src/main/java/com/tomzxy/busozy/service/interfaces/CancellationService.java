package com.tomzxy.busozy.service.interfaces;

import com.tomzxy.busozy.dto.request.CancelBookingReqDTO;
import com.tomzxy.busozy.dto.response.CancellationPreviewResDTO;
import com.tomzxy.busozy.dto.response.CancellationResDTO;

import java.util.UUID;

public interface CancellationService {

    CancellationPreviewResDTO getCancellationPreview(Long userId, UUID bookingCode);

    CancellationResDTO cancelBooking(Long userId, UUID bookingCode, CancelBookingReqDTO req);

    CancellationResDTO getCancellationDetail(Long userId, UUID bookingCode);
}
