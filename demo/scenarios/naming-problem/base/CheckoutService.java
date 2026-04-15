package com.acme.checkout;

import java.math.BigDecimal;

public class CheckoutService {

    public BigDecimal calculateDiscountedTotal(BigDecimal subtotal, BigDecimal campaignDiscount) {
        BigDecimal safeDiscount = campaignDiscount == null ? BigDecimal.ZERO : campaignDiscount;
        return subtotal.subtract(safeDiscount);
    }
}
