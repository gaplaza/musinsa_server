package com.mudosa.musinsa.settlement.domain.vo;

import java.math.BigDecimal;

public class CommissionRate {

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.10");

    public static BigDecimal getDefaultRate() {
        return DEFAULT_RATE;
    }
}