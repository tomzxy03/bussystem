package com.tomzxy.busozy.payment;

/**
 * Returned by PaymentGatewayProvider.initiate() — contains the payment URL and
 * optional QR/deeplink.
 */
public record GatewayInitiateResponse(
        String paymentUrl,
        String qrCodeData,
        String directPayUrl) {
}
