package com.tomzxy.busozy.service.impl.admin;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.common.enums.RefundStatus;
import com.tomzxy.busozy.dto.response.CancellationResDTO;
import com.tomzxy.busozy.entity.Cancellation;
import com.tomzxy.busozy.exception.ResourceNotFoundException;
import com.tomzxy.busozy.mapper.CancellationMapper;
import com.tomzxy.busozy.repository.CancellationRepository;
import com.tomzxy.busozy.service.impl.CancellationRefundProcessor;
import com.tomzxy.busozy.service.interfaces.admin.AdminCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCancellationServiceImpl implements AdminCancellationService {

    private final CancellationRepository cancellationRepository;
    private final CancellationMapper cancellationMapper;
    private final CancellationRefundProcessor refundProcessor;

    @Override
    public Page<CancellationResDTO> getCancellations(String bookingCode, RefundStatus refundStatus, Pageable pageable) {
        UUID parsedBookingCode = parseBookingCode(bookingCode);
        return cancellationRepository.searchAdmin(refundStatus, parsedBookingCode, pageable)
                .map(cancellationMapper::toCancellationRes);
    }

    @Override
    public CancellationResDTO processRefund(Long cancellationId) {
        Cancellation cancellation = cancellationRepository.findById(cancellationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANCELLATION_NOT_FOUND));
        refundProcessor.processRefund(cancellationId);
        return cancellationMapper.toCancellationRes(
                cancellationRepository.findById(cancellation.getId())
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANCELLATION_NOT_FOUND)));
    }

    private UUID parseBookingCode(String bookingCode) {
        if (bookingCode == null || bookingCode.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(bookingCode.trim());
        } catch (IllegalArgumentException ex) {
            throw new com.tomzxy.busozy.exception.BusinessException(ErrorCode.VALIDATION_ERROR, "bookingCode không hợp lệ");
        }
    }
}
