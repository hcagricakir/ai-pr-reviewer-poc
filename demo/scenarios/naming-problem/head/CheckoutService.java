package com.acme.checkout;

import java.math.BigDecimal;

public class CheckoutService {

    public BigDecimal doStuff(BigDecimal x1, BigDecimal tmp) {
        BigDecimal data = tmp == null ? BigDecimal.ZERO : tmp;
        return x1.subtract(data);
    }
}
