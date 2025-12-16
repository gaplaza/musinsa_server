package com.mudosa.musinsa.settlement.domain.service;

import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.exception.BusinessException;
import com.mudosa.musinsa.exception.ErrorCode;
import com.mudosa.musinsa.settlement.domain.model.PgFeePolicy;
import com.mudosa.musinsa.settlement.domain.repository.PgFeePolicyRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class PgFeeCalculator {

    private final PgFeePolicyRepository pgFeePolicyRepository;

    private static final String DEFAULT_PG_PROVIDER = "TOSS";

    private Map<String, List<PgFeePolicy>> policyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    public void refreshCache() {
        List<PgFeePolicy> allPolicies = pgFeePolicyRepository.findAll();

        policyCache.clear();

        allPolicies.stream()
            .filter(PgFeePolicy::isActive)
            .forEach(policy -> {
                String key = buildCacheKey(policy.getPgProvider(), policy.getPaymentMethod());
                policyCache.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(policy);
            });

        log.info("[PgFeeCalculator] 캐시 초기화 완료 - 총 {}건, {}개 조합",
            allPolicies.size(), policyCache.size());
    }

    private String buildCacheKey(String pgProvider, String paymentMethod) {
        return pgProvider + "_" + paymentMethod;
    }

    public Money calculate(String pgProvider, String paymentMethod, Money transactionAmount) {
        return calculate(pgProvider, paymentMethod, transactionAmount, LocalDate.now());
    }

    public Money calculate(
        String pgProvider,
        String paymentMethod,
        Money transactionAmount,
        LocalDate targetDate
    ) {

        if (pgProvider == null) {
            log.warn("⚠️ [PG사 정보 누락] pgProvider가 null입니다. 기본 PG사({}) 사용", DEFAULT_PG_PROVIDER);
            pgProvider = DEFAULT_PG_PROVIDER;
        }

        if (paymentMethod == null) {
            log.error("⚠️ [결제수단 누락] paymentMethod가 null입니다. PG사: {}", pgProvider);
            throw new BusinessException(
                ErrorCode.PG_FEE_POLICY_NOT_FOUND,
                String.format("결제수단이 null입니다. PG사: %s", pgProvider)
            );
        }

        String finalPgProvider = pgProvider;
        String finalPaymentMethod = paymentMethod;

        return findEffectivePolicyFromCache(pgProvider, paymentMethod, targetDate)
            .map(policy -> calculateFromPolicy(policy, transactionAmount))
            .orElseThrow(() -> {
                log.error("⚠️ [PG 수수료 정책 없음] PG사: {}, 결제수단: {}, 날짜: {}",
                    finalPgProvider, finalPaymentMethod, targetDate);
                return new BusinessException(
                    ErrorCode.PG_FEE_POLICY_NOT_FOUND,
                    String.format("PG 수수료 정책을 찾을 수 없습니다. PG사: %s, 결제수단: %s, 날짜: %s",
                        finalPgProvider, finalPaymentMethod, targetDate)
                );
            });
    }

    private Optional<PgFeePolicy> findEffectivePolicyFromCache(
        String pgProvider,
        String paymentMethod,
        LocalDate targetDate
    ) {
        String key = buildCacheKey(pgProvider, paymentMethod);
        List<PgFeePolicy> policies = policyCache.get(key);

        if (policies == null || policies.isEmpty()) {
            return Optional.empty();
        }

        return policies.stream()
            .filter(p -> p.getEffectiveFrom() != null && !p.getEffectiveFrom().isAfter(targetDate))
            .filter(p -> p.getEffectiveTo() == null || !p.getEffectiveTo().isBefore(targetDate))
            .max((a, b) -> a.getEffectiveFrom().compareTo(b.getEffectiveFrom()));
    }

    private Money calculateFromPolicy(PgFeePolicy policy, Money transactionAmount) {
        return switch (policy.getFeeType()) {
            case RATE -> transactionAmount.multiply(policy.getFeeValue()).roundToWon();
            case FIXED -> new Money(policy.getFeeValue());
        };
    }
}
