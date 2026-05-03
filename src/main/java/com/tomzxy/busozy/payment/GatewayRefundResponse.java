package com.tomzxy.busozy.payment;

import java.util.Map;

public record GatewayRefundResponse(
        String transactionId,
        Map<String, Object> rawResponse) {
}
