package com.tomzxy.busozy.event;

import com.tomzxy.busozy.common.enums.BookingStatus;
import com.tomzxy.busozy.common.enums.NotificationChannel;
import com.tomzxy.busozy.entity.Booking;
import com.tomzxy.busozy.repository.BookingRepository;
import com.tomzxy.busozy.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notificationService.sendNotification(
                event.userId(),
                NotificationChannel.SYSTEM,
                "Đặt vé thành công",
                "Đơn đặt vé của bạn đã được xác nhận thanh toán.",
                Map.of("bookingId", event.bookingId(), "type", "BOOKING_CONFIRMED"));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        notificationService.sendNotification(
                event.userId(),
                NotificationChannel.SYSTEM,
                "Thanh toán thành công",
                "Thanh toán cho mã đặt vé " + event.bookingCode() + " đã hoàn tất.",
                Map.of(
                        "paymentId", event.paymentId(),
                        "bookingId", event.bookingId(),
                        "type", "PAYMENT_SUCCESS"));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTripCompleted(TripCompletedEvent event) {
        List<Booking> bookings = bookingRepository.findByTripIdAndStatus(event.tripId(), BookingStatus.COMPLETED);
        for (Booking booking : bookings) {
            notificationService.sendNotification(
                    booking.getUser().getId(),
                    NotificationChannel.SYSTEM,
                    "Chuyến đi đã hoàn thành",
                    "Bạn có thể đánh giá trải nghiệm cho chuyến xe vừa hoàn thành.",
                    Map.of(
                            "tripId", event.tripId(),
                            "bookingId", booking.getId(),
                            "type", "REVIEW_REQUEST"));
        }
    }
}
