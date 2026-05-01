
# ✅ `Master_Plan.md` (Production-Ready)

```markdown
# Master Plan – Busozy Bus Booking System

## Tổng Quan Dự Án
- **Tên**: Busozy – Hệ thống đặt vé xe khách online
- **Stack**: Spring Boot 3.3.5 (LTS) · Java 21 · PostgreSQL · Redis · Flyway · Lombok · MapStruct
- **Architecture**: Layered + Eventual Consistency (non-critical), Strong Consistency (Booking/Payment)

## Database Schema Overview
| Domain       | Tables                                  |
|--------------|-----------------------------------------|
| Core         | `users`, `companies`, `drivers`         |
| Location     | `provinces`, `districts`, `stops`       |
| Route        | `routes`, `route_stops`, `route_prices` |
| Bus          | `bus_types`, `seat_layouts`, `buses`, `seats` |
| Trip         | `trips`, `trip_segments`                |
| Booking      | `bookings`, `booking_seats`, `passengers` |
| Cancellation | `cancellations`, `refund_policies`      |
| Payment      | `payment_methods`, `payments`, `payment_transactions` |
| Promotion    | `promotions`, `promotion_routes`        |
| Support      | `reviews`, `notifications`, `activity_logs` |

## NFR & SLO Checklist
- [ ] Rate Limiting: 100 req/min (public), 1000 req/min (authenticated)
- [ ] Latency: P95 < 500ms (GET), P95 < 2s (POST booking/payment)
- [ ] Availability: 99.9% uptime (SLA)
- [ ] Retention: Booking 7 năm, Audit logs 2 năm, App logs 30 ngày
- [ ] Backup: Daily full + WAL archiving, PITR tested monthly
- [ ] Idempotency: 100% cho mutation endpoints
- [ ] Observability: Tracing enabled, Alerting configured, Log sampling 10% cho high-traffic

## Lộ Trình Triển Khai

### Phase 0 – Infrastructure (Foundation)
`Prompt: prompts/modules/00_infrastructure.md`
- `BaseEntity` + JPA Auditing + `@SQLRestriction`
- `ApiResponse<T>` + `ErrorCode` enum + i18n ready
- `GlobalExceptionHandler` + custom exceptions
- Spring Security + JWT filter + Redis refresh token store
- Redis config + Jackson serializer + Key format: `busozy:{env}:{domain}:{id}`
- Swagger/OpenAPI 3.1 + Actuator health/ready/live
- Correlation ID filter (MDC) + structured JSON logging
- Feature flags config (env-based `@Conditional`)

### Phase 1 – Auth Module
`Prompt: prompts/modules/01_auth.md`
- Entities: `User`, `Role`, `RefreshToken` (Redis-backed)
- APIs: Register, Login, Refresh, Logout, GetMe, UpdateProfile
- Validation: Email/phone unique, password strength, captcha for register
- Idempotency: `POST /auth/register`

### Phase 2 – Location Module
`Prompt: prompts/modules/02_location.md`
- Entities: `Province`, `District`, `Stop`
- APIs: GET provinces, GET districts, GET/POST/PUT stops
- Caching: Redis TTL 24h cho provinces/districts

### Phase 3 – Company & Driver Module
`Prompt: prompts/modules/03_company_driver.md`
- Entities: `Company`, `Driver`
- APIs: GET companies, Admin CRUD drivers
- Validation: License number, company tax code format

### Phase 4 – Route Module
`Prompt: prompts/modules/04_route.md`
- Entities: `Route`, `RouteStop`, `RoutePrice`
- APIs: Search routes, GET detail/prices, Admin CRUD
- Pricing Rule: `pickup_stop_order < dropoff_stop_order`. Không dùng `stop_id` để tính giá.

### Phase 5 – Bus Module
`Prompt: prompts/modules/05_bus.md`
- Entities: `BusType`, `SeatLayout`, `Bus`, `Seat`
- APIs: GET bus-types, Admin buses, GET seat layout
- JSONB: `seat_layout` lưu dạng `{"rows": [...], "config": "..."}`

### Phase 6 – Trip Module ⭐ (Core)
`Prompt: prompts/modules/06_trip.md`
- Entities: `Trip`, `TripSegment`
- Redis: `busozy:{env}:trip_seats:{tripId}` (Sorted Set cache availability)
- APIs: Search trips, GET detail, GET seats/status, Admin CRUD
- Logic:
  - Availability from DB `booking_seats` JOIN `bookings` (truth source).
  - Redis cache chỉ để read fast, refresh every 30s or on booking event.
  - Segment overlap: `NOT (dropoff <= user_pickup OR pickup >= user_dropoff)`
  - New API: `GET /trips/{id}/availability?pickup={order}&dropoff={order}`

### Phase 7 – Booking Module ⭐⭐ (Core Business)
`Prompt: prompts/modules/07_booking.md`
- Entities: `Booking`, `BookingSeat`, `Passenger`
- Redis: Pessimistic lock `SETNX busozy:{env}:seat_lock:{seatId}` TTL 10m
- APIs: Create, GET by code, GET my bookings, Confirm, Cancel
- Concurrency Strategy:
  1. Redis lock (UX reservation)
  2. DB `@Version` optimistic lock on `seats`/`trip`
  3. Re-check availability inside `@Transactional`
  4. Scheduled job cleanup expired locks + publish release event
- Transaction Boundary: Create booking + reserve seats trong 1 TX. Payment init bên ngoài TX → dùng Saga/compensating nếu fail.
- Idempotency: `Idempotency-Key` required. Cache response 24h.

### Phase 8 – Payment Module
`Prompt: prompts/modules/08_payment.md`
- Entities: `PaymentMethod`, `Payment`, `PaymentTransaction`
- APIs: GET methods, Initiate, Webhooks (MoMo/VNPay), GET status
- Webhook Security: Verify signature/HMAC, IP whitelist, retry logic.
- State Machine: `PENDING → PROCESSING → SUCCESS/FAILED → REFUNDED`
- Idempotent handler: `payment_id + external_txn_id` unique constraint.

### Phase 9 – Promotion Module
`Prompt: prompts/modules/09_promotion.md`
- Entities: `Promotion`, `PromotionRoute`
- APIs: Validate, Admin CRUD
- Rules: Expiry, usage limit, route restriction, stackable=false

### Phase 10 – Review & Notification Module
`Prompt: prompts/modules/10_review_notification.md`
- APIs: POST review (after trip completed), GET trip reviews, GET/patch notifications
- Async: Spring `@Async` hoặc event listener cho notification dispatch

### Phase 11 – Cancellation & Refund Module
`Prompt: prompts/modules/11_cancellation.md`
- Entities: `Cancellation`, `RefundPolicy`
- Logic: Auto-calculate refund % based on `departure_time - cancel_time`
- Flow: Update booking → trigger refund → update payment → notify

### Phase 12 – Admin & Dashboard Module
`Prompt: prompts/modules/12_admin.md`
- Aggregate admin endpoints, RBAC enforcement
- Stats: Bookings count, revenue, active trips, occupancy rate
- Moderation: Block users, hide spam reviews, audit logs view

## Dependency Graph & Execution Order

## Dependency Graph
Infrastructure (Phase 0)
    └── Auth (Phase 1)
            └── Location (Phase 2) + Company/Driver (Phase 3)
                    └── Route (Phase 4)
                            └── Bus (Phase 5)
                                    └── Trip (Phase 6)
                                            └── Booking (Phase 7)
                                                    ├── Payment (Phase 8)
                                                    ├── Promotion (Phase 9)
                                                    ├── Review & Notification (Phase 10)
                                                    └── Cancellation (Phase 11)






### Triển Khai Theo Cụm
| Cụm | Phases | Mục tiêu |
|-----|--------|----------|
| 🏗️ 0 – Foundation | 0, 1 | Core infra, security, logging ready |
| 📦 1 – Master Data | 2–6 | Đủ data để tạo & query Trip |
| ⚙️ 2 – Core Ops | 7, 8 | Luồng đặt vé + thanh toán chạy end-to-end |
| 🔌 3 – Extensions | 9–12 | Promo, review, cancel, admin panel |

## Hướng Dẫn Sử Dụng Prompt
1. Luôn đọc `global_standards.md` trước khi code.
2. Mỗi module prompt chứa: Entity spec, DTOs, API contract, business rules, test cases.
3. Sau mỗi phase: `mvn clean verify` → chạy integration test → deploy staging → smoke test.
4. Ghi ADR (Architecture Decision Record) cho các choice quan trọng (e.g., lock strategy, cache pattern).
5. Code review checklist phải pass 100% trước khi merge.