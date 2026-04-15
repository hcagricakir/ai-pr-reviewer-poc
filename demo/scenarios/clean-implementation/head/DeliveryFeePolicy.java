package com.acme.pricing;

import java.math.BigDecimal;

public record DeliveryFeePolicy(BigDecimal freeDeliveryThreshold, BigDecimal standardDeliveryFee) {

    public BigDecimal feeFor(BigDecimal orderTotal) {
        if (orderTotal == null) {
            throw new IllegalArgumentException("orderTotal must not be null");
        }
        if (orderTotal.compareTo(freeDeliveryThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return standardDeliveryFee;
    }
}
