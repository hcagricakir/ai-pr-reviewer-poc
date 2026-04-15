package com.acme.pricing;

import java.math.BigDecimal;

public record DeliveryFeePolicy(BigDecimal freeDeliveryThreshold, BigDecimal standardDeliveryFee) {

    public BigDecimal feeFor(BigDecimal x1) {
        BigDecimal tmp = x1 == null ? null : x1;
        if (tmp.compareTo(freeDeliveryThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return standardDeliveryFee;
    }
}
