package com.tomzxy.busozy.common.enums;

import java.util.Map;
import java.util.Set;

public enum TripStatus {
    DRAFT, SCHEDULED, DELAYED, DEPARTED, COMPLETED, CANCELLED;

    // Luật chuyển trạng thái hợp lệ
    private static final Map<TripStatus, Set<TripStatus>> ALLOWED_TRANSITIONS = Map.of(
        DRAFT,      Set.of(SCHEDULED, CANCELLED),
        SCHEDULED,  Set.of(DELAYED, DEPARTED, CANCELLED),
        DELAYED,    Set.of(DEPARTED, CANCELLED),
        DEPARTED,   Set.of(COMPLETED, CANCELLED), // Hủy khẩn cấp sau khi xuất bến
        COMPLETED,  Set.of(),                     // Trạng thái cuối
        CANCELLED,  Set.of()                      // Trạng thái cuối
    );

    /**
     * Kiểm tra trạng thái hiện tại có được chuyển sang `next` không
     */
    public boolean canTransitionTo(TripStatus next) {
        if (next == null) return false;
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
