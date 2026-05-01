package com.tomzxy.busozy.common.enums;

public enum ErrorCode {

    // Auth
    USER_NOT_FOUND("AUTH_001", "Người dùng không tồn tại"),
    INVALID_CREDENTIALS("AUTH_002", "Thông tin đăng nhập không hợp lệ"),
    EMAIL_ALREADY_EXISTS("AUTH_003", "Email đã được sử dụng"),
    USERNAME_ALREADY_EXISTS("AUTH_004", "Tên đăng nhập đã được sử dụng"),
    PHONE_ALREADY_EXISTS("AUTH_005", "Số điện thoại đã được sử dụng"),
    ACCOUNT_INACTIVE("AUTH_006", "Tài khoản chưa được kích hoạt hoặc đã bị khóa"),
    TOKEN_INVALID("AUTH_007", "Token không hợp lệ"),
    TOKEN_EXPIRED("AUTH_008", "Token đã hết hạn"),
    IDEMPOTENCY_DUPLICATE("AUTH_009", "Yêu cầu đã được xử lý trước đó"),

    // Payment
    PAYMENT_NOT_FOUND("PAY_001", "Không tìm thấy thông tin thanh toán"),
    PAYMENT_FAILED("PAY_002", "Thanh toán thất bại"),

    // Location
    PROVINCE_NOT_FOUND("LOC_001", "Tỉnh/thành phố không tồn tại"),
    DISTRICT_NOT_FOUND("LOC_002", "Quận/huyện không tồn tại"),
    DISTRICT_PROVINCE_MISMATCH("LOC_003", "Quận không thuộc tỉnh được chọn"),
    STOP_CODE_EXISTS("LOC_004", "Mã điểm dừng đã tồn tại"),
    INVALID_COORDINATES("LOC_005", "Tọa độ không hợp lệ"),
    STOP_NOT_FOUND("LOC_006", "Điểm dừng không tồn tại"),

    // Company
    COMPANY_NOT_FOUND("COMP_001", "Công ty không tồn tại"),
    TAX_CODE_EXISTS("COMP_002", "Mã số thuế đã được sử dụng"),
    COMPANY_NOT_ACTIVE("COMP_003", "Công ty chưa được kích hoạt"),
    COMPANY_HAS_ACTIVE_BUSES("COMP_004", "Không thể xóa công ty đang có xe hoạt động"),

    // Driver
    DRIVER_NOT_FOUND("DRV_001", "Tài xế không tồn tại"),
    LICENSE_EXISTS_IN_COMPANY("DRV_002", "Số GPLX đã tồn tại trong công ty"),
    PHONE_EXISTS_IN_COMPANY("DRV_003", "Số điện thoại đã tồn tại trong công ty"),
    COMPANY_NOT_FOUND_FOR_DRIVER("DRV_004", "Công ty của tài xế không tồn tại"),

    // Route
    ROUTE_NOT_FOUND("ROUTE_001", "Tuyến đường không tồn tại"),
    ROUTE_CODE_EXISTS("ROUTE_002", "Mã tuyến đã được sử dụng"),
    INVALID_STOP_SEQUENCE("ROUTE_003", "Thứ tự điểm dừng không hợp lệ hoặc trùng lặp"),
    ORIGIN_DEST_NOT_IN_ROUTE("ROUTE_004", "Điểm đi/đến không thuộc danh sách dừng của tuyến"),
    INVALID_PRICE_SEGMENT("ROUTE_005", "pickup_order phải nhỏ hơn dropoff_order"),
    ROUTE_COMPANY_NOT_ACTIVE("ROUTE_006", "Công ty chủ quản chưa được kích hoạt"),

    // Bus
    BUS_TYPE_NOT_FOUND("BUS_001", "Loại xe không tồn tại"),
    BUS_TYPE_HAS_ACTIVE_BUSES("BUS_002", "Không thể xóa loại xe đang có xe hoạt động"),
    LICENSE_PLATE_EXISTS("BUS_003", "Biển số xe đã tồn tại"),
    INVALID_SEAT_LAYOUT("BUS_004", "Dữ liệu sơ đồ ghế không hợp lệ"),
    BUS_NOT_FOUND("BUS_005", "Xe không tồn tại"),
    SEAT_LAYOUT_NOT_FOUND("BUS_006", "Sơ đồ ghế không tồn tại"),

    // Seat
    SEAT_NUMBER_DUPLICATE("SEAT_001", "Mã ghế trùng trong cùng xe"),
    SEAT_LAYOUT_MISMATCH("SEAT_002", "Sơ đồ ghế không thuộc loại xe đã chọn"),
    INVALID_PRICE_MULTIPLIER("SEAT_003", "Hệ số giá nằm ngoài khoảng 0.1 - 5.0"),

    // Trip
    TRIP_NOT_FOUND("TRIP_001", "Chuyến xe không tồn tại"),
    BUS_TIME_CONFLICT("TRIP_002", "Xe đã được gán cho chuyến khác trong cùng khung giờ"),
    DRIVER_COMPANY_MISMATCH("TRIP_003", "Tài xế không thuộc công ty quản lý chuyến"),
    ROUTE_STOP_NOT_FOUND("TRIP_004", "Điểm đi/đến không thuộc tuyến đường"),
    CANNOT_DELETE_SCHEDULED_TRIP("TRIP_005", "Chỉ được xóa chuyến ở trạng thái DRAFT"),
    SEGMENT_FULLY_BOOKED("TRIP_006", "Phân đoạn ghế đã hết chỗ"),
    INVALID_STATUS_TRANSITION("TRIP_007", "Chuyển trạng thái chuyến xe không hợp lệ"),

    // Booking
    BOOKING_NOT_FOUND("BOOKING_001", "Mã đặt vé không tồn tại"),
    SEATS_FULL_OR_OVERLAP("BOOKING_002", "Ghế đã được đặt hoặc không khả dụng trên đoạn đường này"),
    SEAT_LOCKED("BOOKING_003", "Ghế đang được giữ bởi người dùng khác, vui lòng thử lại sau"),
    INVALID_CANCEL_STATUS("BOOKING_005", "Không thể hủy vé ở trạng thái này"),
    TRIP_NOT_ACTIVE("BOOKING_006", "Chuyến xe không tồn tại hoặc đã hủy"),
    ROUTE_PRICE_NOT_FOUND("BOOKING_007", "Không tìm thấy giá cho đoạn đường này"),
    BOOKING_CONFLICT("BOOKING_008", "Xung đột đặt chỗ"),

    // Payment
    PAYMENT_BOOKING_INVALID("PAY_001", "Booking không tồn tại hoặc không thể thanh toán"),
    PAYMENT_METHOD_UNSUPPORTED("PAY_002", "Phương thức thanh toán không được hỗ trợ"),
    PAYMENT_INVALID_SIGNATURE("PAY_003", "Chữ ký webhook không hợp lệ"),
    PAYMENT_ALREADY_PROCESSED("PAY_004", "Giao dịch đã được xử lý trước đó"),
    PAYMENT_AMOUNT_MISMATCH("PAY_005", "Số tiền thanh toán không khớp với giá vé"),
    PAYMENT_GATEWAY_TIMEOUT("PAY_006", "Cổng thanh toán phản hồi chậm hoặc lỗi"),

    // Promotion
    PROMOTION_NOT_FOUND("PROMO_001", "Mã khuyến mãi không tồn tại hoặc không hoạt động"),
    PROMOTION_EXPIRED("PROMO_002", "Mã khuyến mãi đã hết hạn"),
    PROMOTION_USAGE_EXCEEDED("PROMO_003", "Mã đã hết lượt sử dụng"),
    PROMOTION_USER_LIMIT_EXCEEDED("PROMO_004", "Bạn đã dùng mã này tối đa số lần cho phép"),
    PROMOTION_MIN_ORDER_NOT_MET("PROMO_005", "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã"),
    PROMOTION_ROUTE_NOT_APPLICABLE("PROMO_006", "Mã không áp dụng cho tuyến đường này"),
    PROMOTION_ALREADY_APPLIED("PROMO_007", "Mã đã được áp dụng cho đơn hàng này"),

    // General
    RESOURCE_NOT_FOUND("GEN_001", "Không tìm thấy tài nguyên"),
    VALIDATION_ERROR("GEN_002", "Dữ liệu đầu vào không hợp lệ"),
    ACCESS_DENIED("GEN_003", "Không có quyền truy cập"),
    INTERNAL_ERROR("GEN_500", "Lỗi hệ thống nội bộ");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
