1. Cập nhật PromotionRepository.java
Thêm method decrementUsage để giảm số lượt dùng mã khuyến mãi.
```java
package com.tomzxy.busozy.repository;

import com.tomzxy.busozy.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

    // Logic tăng (đã có)
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount + 1 WHERE p.id = :id AND p.usedCount < p.usageLimit")
    int incrementUsage(Long id);

    // 🔴 FIX: Logic giảm khi hủy vé
    // Giảm usedCount nhưng không được nhỏ hơn 0
    @Modifying
    @Query("UPDATE Promotion p SET p.usedCount = p.usedCount - 1 WHERE p.id = :id AND p.usedCount > 0")
    int decrementUsage(Long id);
}
```
2. Cập nhật CancellationServiceImpl.java
Thêm logic rollback promotion và capping refund amount vào luồng hủy vé.
```java
package com.tomzxy.busozy.service.impl;

import com.tomzxy.busozy.common.enums.ErrorCode;
import com.tomzxy.busozy.dto.request.CancelBookingReqDTO;
import com.tomzxy.busozy.dto.response.CancellationResDTO;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.entity.Cancellation;
import com.tomzxy.busozy.entity.CancellationPolicy;
import com.tomzxy.busozy.entity.Payment;
import com.tomzxy.busozy.enums.BookingStatus;
import com.tomzxy.busozy.enums.PaymentStatus;
import com.tomzxy.busozy.enums.RefundStatus;
import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.repository.CancellationRepository;
import com.tomzxy.busozy.repository.PromotionRepository;
import com.tomzxy.busozy.service.interfaces.CancellationService;
import com.tomzxy.busozy.service.interfaces.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class CancellationServiceImpl implements CancellationService {

    private final CancellationRepository cancellationRepository;
    private final BookingRepository bookingRepository;
    private final PromotionRepository promotionRepository; // Inject repo để decrement
    private final PromotionService promotionService;       // Inject service nếu cần logic phức tạp
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CancellationResDTO cancelBooking(Long userId, String bookingCode, CancelBookingReqDTO req) {
        // 1. Validate Booking
        Booking booking = bookingRepository.findByBookingCodeAndUserId(bookingCode, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_001));

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.CANCEL_003);
        }

        // 2. Resolve Policy & Calculate Refund
        CancellationPolicy policy = resolvePolicy(booking);
        BigDecimal calculatedRefund = calculateRefundAmount(booking, policy);

        // 🔴 FIX ITEM #2: Capping Refund Amount
        // Số tiền hoàn không được vượt quá số tiền thực tế đã thanh toán
        BigDecimal maxRefund = BigDecimal.ZERO;
        if (booking.getPaymentStatus() == PaymentStatus.PAID && booking.getPayment() != null) {
            maxRefund = booking.getPayment().getAmount();
        }
        BigDecimal finalRefundAmount = calculatedRefund.min(maxRefund);

        // 3. Update Booking Status
        booking.setStatus(BookingStatus.CANCELLED);
        if (finalRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        bookingRepository.save(booking);

        // 4. Create Cancellation Record
        Cancellation cancellation = new Cancellation();
        cancellation.setBooking(booking);
        cancellation.setCancelledBy(/* Lấy user từ SecurityContext */);
        cancellation.setCancelReason(req.getReason());
        cancellation.setCancelTime(OffsetDateTime.now());
        cancellation.setRefundAmount(finalRefundAmount);
        cancellation.setRefundStatus(finalRefundAmount.compareTo(BigDecimal.ZERO) > 0 
                ? RefundStatus.PENDING : RefundStatus.COMPLETED);
        cancellationRepository.save(cancellation);

        // 🔴 FIX ITEM #1: Promotion Rollback
        // Nếu booking có dùng mã khuyến mãi, hoàn trả lại lượt dùng
        if (booking.getPromotionId() != null) {
            int updated = promotionRepository.decrementUsage(booking.getPromotionId());
            if (updated == 0) {
                // Log warning nếu không giảm được (edge case hiếm)
                // Có thể cần xử lý thêm nếu logic strict
            }
            
            // Nếu có bảng UserPromotionUsage, cũng cần decrement ở bảng đó
            // userPromotionUsageRepository.decrementUsage(userId, booking.getPromotionId());
        }

        // 5. Trigger Refund Event (Async)
        if (finalRefundAmount.compareTo(BigDecimal.ZERO) > 0 && booking.getPaymentStatus() == PaymentStatus.PAID) {
            eventPublisher.publishEvent(new RefundRequestedEvent(cancellation.getId(), booking.getPayment().getGatewayTransactionId(), finalRefundAmount));
        }

        return mapToResponse(cancellation);
    }
    
    // ... các method calculateRefundAmount, resolvePolicy ...
}
```
3. Giải thích kỹ thuật cho Fix này
ACID Transaction:
Toàn bộ logic trên nằm trong @Transactional. Nếu promotionRepository.decrementUsage thất bại (ví dụ lỗi DB), thì transaction sẽ rollback → Booking cũng không bị hủy sai. Điều này đảm bảo dữ liệu đồng nhất tuyệt đối.
Refund Capping (Item #2):
Code calculatedRefund.min(maxRefund) đảm bảo rằng dù chính sách hoàn tiền có tính ra bao nhiêu đi nữa, bạn cũng không bao giờ trả lại cho User nhiều hơn số tiền họ đã thực sự trả (đặc biệt quan trọng khi tích hợp cổng thanh toán tính phí).
Promotion Safety (Item #1):
Query UPDATE ... SET usedCount = usedCount - 1 WHERE id = ? AND usedCount > 0 an toàn hơn là UPDATE ... SET usedCount = usedCount - 1 bình thường. Nó đảm bảo số lượt dùng không bao giờ bị âm (ví dụ: -1) do lỗi concurrent nào đó.