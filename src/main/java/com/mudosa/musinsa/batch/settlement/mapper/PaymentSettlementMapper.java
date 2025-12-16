package com.mudosa.musinsa.batch.settlement.mapper;

import com.mudosa.musinsa.batch.settlement.dto.PaymentSettlementDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentSettlementMapper {

    List<PaymentSettlementDto> findPendingPaymentsWithBrandAmount(
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countPendingPayments();

    void markSettledByIds(@Param("paymentIds") List<Long> paymentIds);
}