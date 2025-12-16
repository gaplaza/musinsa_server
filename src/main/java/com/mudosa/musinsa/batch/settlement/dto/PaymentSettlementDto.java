package com.mudosa.musinsa.batch.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSettlementDto {
    private Long paymentId;
    private String pgTransactionId;
    private String pgProvider;
    private String paymentMethod;

    private Long orderId;

    private Long brandId;
    private BigDecimal totalAmount;
    private BigDecimal commissionRate;
}