package com.acme.pricing;

import java.math.BigDecimal;

public record DeliveryFeePolicy(BigDecimal freeDeliveryThreshold, BigDecimal standardDeliveryFee) {

    public BigDecimal feeFor(BigDecimal orderTotal) {
        BigDecimal safeTotal = orderTotal == null ? null : orderTotal;
        if (safeTotal.compareTo(freeDeliveryThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return standardDeliveryFee;
    }
}
