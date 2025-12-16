package com.mudosa.musinsa.settlement.application;

import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.settlement.domain.model.SettlementPerTransaction;
import com.mudosa.musinsa.settlement.domain.model.TransactionType;
import com.mudosa.musinsa.settlement.domain.repository.SettlementPerTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementApplicationService {

    private final SettlementPerTransactionRepository settlementPerTransactionRepository;

    @Transactional
    public SettlementPerTransaction createSettlementTransaction(
        Long brandId,
        Long paymentId,
        String pgTransactionId,
        Money transactionAmount,
        BigDecimal commissionRate,
        Money pgFeeAmount,
        TransactionType transactionType,
        String timezoneOffset
    ) {
        log.info("Creating settlement transaction for payment_id={}, amount={}", paymentId, transactionAmount);

        SettlementPerTransaction settlement = SettlementPerTransaction.create(
            brandId,
            paymentId,
            pgTransactionId,
            transactionAmount,
            commissionRate,
            pgFeeAmount,
            transactionType,
            timezoneOffset
        );

        SettlementPerTransaction saved = settlementPerTransactionRepository.save(settlement);

        log.info("Settlement transaction created: id={}, final_amount={}",
            saved.getId(), saved.calculateFinalSettlementAmount());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<SettlementPerTransaction> getSettlementTransactions(
        Long brandId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        log.info("Fetching settlement transactions for brand_id={}, period={} to {}",
            brandId, startDate, endDate);

        return settlementPerTransactionRepository.findByBrandIdAndTransactionDateLocalBetween(brandId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public SettlementPerTransaction getSettlementTransaction(Long settlementId) {
        return settlementPerTransactionRepository.findById(settlementId)
            .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));
    }

    @Transactional(readOnly = true)
    public SettlementPerTransaction getSettlementTransactionByPaymentId(Long paymentId) {
        return settlementPerTransactionRepository.findFirstByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("Settlement not found for payment: " + paymentId));
    }

    @Transactional(readOnly = true)
    public boolean existsByPaymentId(Long paymentId) {
        log.debug("Checking if settlement exists for paymentId={}", paymentId);
        return settlementPerTransactionRepository.findFirstByPaymentId(paymentId).isPresent();
    }
}