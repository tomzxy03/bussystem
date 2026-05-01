# 4 Critical Production Checks
1️⃣ Circular Dependency & ApplicationContext Anti-Pattern
Vấn đề: Walkthrough ghi "gọi promotionService.incrementUsage qua ApplicationContext.getBean() trong confirmPayment". Đây là dấu hiệu của Circular Dependency (BookingService ↔ PromotionService). Dùng ApplicationContext chỉ là workaround, khó test và vi phạm nguyên tắc DI.
✅ Giải pháp chuẩn: Dùng Spring Events (đã có sẵn từ Phase 8).
```java
// Trong PaymentServiceImpl (sau khi update booking PAID)
eventPublisher.publishEvent(new BookingConfirmedEvent(booking.getId(), booking.getPromotionId()));

// PromotionListener.java
@Component @RequiredArgsConstructor
public class PromotionUsageListener {
    private final PromotionService promotionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        if (event.promotionId() != null) {
            promotionService.incrementUsage(event.promotionId(), event.bookingId());
        }
    }
}
```
→ Tách biệt hoàn toàn, không vòng lặp dependency, tự động rollback nếu increment fail.
2️⃣ Xử Lý Trường Hợp Hết Limit Đúng Lúc Confirm
Vấn đề: Query optimistic UPDATE ... WHERE used_count < usage_limit trả về rowCount == 0 nghĩa là limit đã đầy giữa lúc validate và thanh toán.
✅ Policy đề xuất: Fail fast & Rollback. Không cho phép booking confirm với discount ảo.
```java
int updated = promotionRepository.incrementUsage(promotionId);
if (updated == 0) {
    throw new BusinessException(ErrorCode.PROMO_003); // Trigger rollback transaction
}
```
3️⃣ M2M Route Check Hiệu Năng
Vấn đề: Check route scope applicability bằng cách load Set<Route> applicableRoutes gây N+1 hoặc memory waste.
✅ Tối ưu: Dùng repository existsBy trực tiếp.
```java
// PromotionRepository.java
@Query("SELECT COUNT(pr) > 0 FROM PromotionRoute pr WHERE pr.promotion.id = :promoId AND pr.route.id = :routeId")
boolean existsByPromotionIdAndRouteId(@Param("promoId") Long promoId, @Param("routeId") Long routeId);

// Service validation
if (!promo.getApplicableRoutes().isEmpty() && 
    !promoRepository.existsByPromotionIdAndRouteId(promo.getId(), routeId)) {
    return PromotionValidateResDTO.of(false, "Mã không áp dụng cho tuyến này");
}
```
4️⃣ Timezone Handling Cho Validity Window
Vấn đề: valid_from/valid_to là OffsetDateTime. So sánh với OffsetDateTime.now() có thể sai lệch múi giờ server.
✅ Fix: Luôn ép về Asia/Ho_Chi_Minh.
```java
ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
OffsetDateTime now = OffsetDateTime.now(VN_ZONE);
if (now.isBefore(promo.getValidFrom()) || now.isAfter(promo.getValidTo())) { ... }
```
