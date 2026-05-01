package com.tomzxy.busozy.payment;

import com.tomzxy.busozy.exception.BusinessException;
import com.tomzxy.busozy.common.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves the appropriate PaymentGatewayProvider for a given method code.
 * All providers are injected as a list by Spring (Strategy factory).
 */
@Component
@RequiredArgsConstructor
public class GatewayFactory {

    private final List<PaymentGatewayProvider> providers;

    /**
     * Returns the first provider that supports the given methodCode.
     * 
     * @throws BusinessException(PAY_002) if no provider matches.
     */
    public PaymentGatewayProvider getProvider(String methodCode) {
        return providers.stream()
                .filter(p -> p.supports(methodCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_UNSUPPORTED));
    }
}
