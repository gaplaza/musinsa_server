package com.mudosa.musinsa.settlement.application;

import com.mudosa.musinsa.brand.domain.model.Brand;
import com.mudosa.musinsa.brand.domain.repository.BrandRepository;
import com.mudosa.musinsa.common.vo.Money;
import com.mudosa.musinsa.exception.BusinessException;
import com.mudosa.musinsa.exception.ErrorCode;
import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementWeekly;
import com.mudosa.musinsa.settlement.domain.model.SettlementYearly;
import com.mudosa.musinsa.settlement.domain.model.SettlementPerTransaction;
import com.mudosa.musinsa.settlement.domain.repository.SettlementDailyRepository;
import com.mudosa.musinsa.settlement.domain.repository.SettlementMonthlyRepository;
import com.mudosa.musinsa.settlement.domain.repository.SettlementPerTransactionRepository;
import com.mudosa.musinsa.settlement.domain.repository.SettlementWeeklyRepository;
import com.mudosa.musinsa.settlement.domain.repository.SettlementYearlyRepository;
import com.mudosa.musinsa.settlement.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService {

    private final SettlementDailyRepository dailyRepository;
    private final SettlementWeeklyRepository weeklyRepository;
    private final SettlementMonthlyRepository monthlyRepository;
    private final SettlementYearlyRepository yearlyRepository;
    private final SettlementPerTransactionRepository perTransactionRepository;
    private final BrandRepository brandRepository;

    
    public Page<SettlementDailyResponse> getDailySettlements(Long brandId, Pageable pageable) {
        log.info("일일 정산 목록 조회 - brandId: {}", brandId);

        Page<SettlementDaily> settlements = dailyRepository.findAllWithFilters(
            brandId,
            null,
            null,
            null,
            null,
            pageable
        );

        return settlements.map(settlement -> {
            String brandName = getBrandName(settlement.getBrandId());
            return SettlementDailyResponse.from(settlement, brandName);
        });
    }

    
    public SettlementDailyResponse getDailySettlement(Long settlementDailyId) {
        log.info("일일 정산 상세 조회 - id: {}", settlementDailyId);

        SettlementDaily settlement = dailyRepository.findById(settlementDailyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        String brandName = getBrandName(settlement.getBrandId());

        return SettlementDailyResponse.from(settlement, brandName);
    }

    
    public Page<SettlementWeeklyResponse> getWeeklySettlements(Long brandId, Pageable pageable) {
        log.info("주간 정산 목록 조회 - brandId: {}", brandId);

        List<SettlementWeekly> allSettlements = weeklyRepository.findByBrandIdOrderByWeekStartDateDesc(brandId);
        String brandName = getBrandName(brandId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allSettlements.size());

        List<SettlementWeeklyResponse> responseList = allSettlements.subList(start, end)
            .stream()
            .map(settlement -> SettlementWeeklyResponse.from(settlement, brandName))
            .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, allSettlements.size());
    }

    
    public SettlementWeeklyResponse getWeeklySettlement(Long settlementWeeklyId) {
        log.info("주간 정산 상세 조회 - id: {}", settlementWeeklyId);

        SettlementWeekly settlement = weeklyRepository.findById(settlementWeeklyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        String brandName = getBrandName(settlement.getBrandId());

        return SettlementWeeklyResponse.from(settlement, brandName);
    }

    
    public Page<SettlementMonthlyResponse> getMonthlySettlements(Long brandId, Pageable pageable) {
        log.info("월간 정산 목록 조회 - brandId: {}", brandId);

        Page<SettlementMonthly> settlements = monthlyRepository.findAllWithFilters(
            brandId,
            null,
            null,
            null,
            null,
            pageable
        );

        return settlements.map(settlement -> {
            String brandName = getBrandName(settlement.getBrandId());
            return SettlementMonthlyResponse.from(settlement, brandName);
        });
    }

    
    public SettlementMonthlyResponse getMonthlySettlement(Long settlementMonthlyId) {
        log.info("월간 정산 상세 조회 - id: {}", settlementMonthlyId);

        SettlementMonthly settlement = monthlyRepository.findById(settlementMonthlyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        String brandName = getBrandName(settlement.getBrandId());

        return SettlementMonthlyResponse.from(settlement, brandName);
    }

    
    public Page<SettlementYearlyResponse> getYearlySettlements(Long brandId, Pageable pageable) {
        log.info("연간 정산 목록 조회 - brandId: {}", brandId);

        List<SettlementYearly> allSettlements = yearlyRepository.findByBrandIdOrderBySettlementYearDesc(brandId);
        String brandName = getBrandName(brandId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allSettlements.size());

        List<SettlementYearlyResponse> responseList = allSettlements.subList(start, end)
            .stream()
            .map(settlement -> SettlementYearlyResponse.from(settlement, brandName))
            .collect(Collectors.toList());

        return new PageImpl<>(responseList, pageable, allSettlements.size());
    }

    
    public SettlementYearlyResponse getYearlySettlement(Long settlementYearlyId) {
        log.info("연간 정산 상세 조회 - id: {}", settlementYearlyId);

        SettlementYearly settlement = yearlyRepository.findById(settlementYearlyId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        String brandName = getBrandName(settlement.getBrandId());

        return SettlementYearlyResponse.from(settlement, brandName);
    }

    
    public Page<SettlementPerTransactionResponse> getPerTransactionSettlements(
        Long brandId,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    ) {
        log.info("거래별 정산 목록 조회 - brandId: {}, startDate: {}, endDate: {}",
            brandId, startDate, endDate);

        Page<SettlementPerTransaction> transactions = perTransactionRepository
            .findByBrandIdAndTransactionDateLocalBetween(brandId, startDate, endDate, pageable);

        String brandName = getBrandName(brandId);

        return transactions.map(tx -> SettlementPerTransactionResponse.from(tx, brandName));
    }

    
    public SettlementPerTransactionResponse getPerTransactionSettlement(Long transactionId) {
        log.info("거래별 정산 단건 조회 - id: {}", transactionId);

        SettlementPerTransaction transaction = perTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        String brandName = getBrandName(transaction.getBrandId());

        return SettlementPerTransactionResponse.from(transaction, brandName);
    }

    
    public SettlementStatisticsResponse getStatistics(Long brandId) {
        log.info("정산 통계 조회 - brandId: {}", brandId);

        String brandName = (brandId == null) ? "전체 브랜드" : getBrandName(brandId);
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        SettlementStatistics todayStats = getTodayStatistics(brandId, today);
        SettlementStatistics weekStats = getWeekStatistics(brandId, today);
        SettlementStatistics monthStats = getMonthStatistics(brandId, currentYear, currentMonth);
        SettlementStatistics yearStats = getYearStatistics(brandId, currentYear);
        SettlementStatistics totalStats = getTotalStatistics(brandId);

        return SettlementStatisticsResponse.of(
            brandId != null ? brandId : 0L,
            brandName,
            todayStats.salesAmount,
            todayStats.orderCount,
            weekStats.salesAmount,
            weekStats.orderCount,
            monthStats.salesAmount,
            monthStats.orderCount,
            yearStats.salesAmount,
            yearStats.orderCount,
            totalStats.salesAmount,
            totalStats.orderCount
        );
    }

    
    private SettlementStatistics getTodayStatistics(Long brandId, LocalDate today) {
        if (brandId == null) {
            Map<String, Object> stats = dailyRepository.sumAllBySettlementDate(today);
            return SettlementStatistics.fromMap(stats);
        }

        return dailyRepository
            .findByBrandIdAndSettlementDate(brandId, today)
            .map(SettlementStatistics::from)
            .orElseGet(SettlementStatistics::empty);
    }

    
    private SettlementStatistics getWeekStatistics(Long brandId, LocalDate today) {
        WeekFields weekFields = WeekFields.ISO;
        int year = today.getYear();
        int month = today.getMonthValue();
        int weekOfMonth = today.get(weekFields.weekOfMonth());

        if (brandId == null) {
            Map<String, Object> stats = weeklyRepository.sumAllByYearAndMonthAndWeekOfMonth(year, month, weekOfMonth);
            return SettlementStatistics.fromMap(stats);
        }

        return weeklyRepository
            .findByBrandIdAndSettlementYearAndSettlementMonthAndWeekOfMonth(brandId, year, month, weekOfMonth)
            .map(SettlementStatistics::from)
            .orElseGet(SettlementStatistics::empty);
    }

    
    private SettlementStatistics getMonthStatistics(Long brandId, int year, int month) {
        if (brandId == null) {
            Map<String, Object> stats = monthlyRepository.sumAllByYearAndMonth(year, month);
            return SettlementStatistics.fromMap(stats);
        }

        return monthlyRepository
            .findByBrandIdAndSettlementYearAndSettlementMonth(brandId, year, month)
            .map(SettlementStatistics::from)
            .orElseGet(SettlementStatistics::empty);
    }

    
    private SettlementStatistics getYearStatistics(Long brandId, int year) {
        if (brandId == null) {
            Map<String, Object> stats = yearlyRepository.sumAllByYear(year);
            return SettlementStatistics.fromMap(stats);
        }

        return yearlyRepository
            .findByBrandIdAndSettlementYear(brandId, year)
            .map(SettlementStatistics::from)
            .orElseGet(SettlementStatistics::empty);
    }

    
    private SettlementStatistics getTotalStatistics(Long brandId) {
        if (brandId == null) {
            Map<String, Object> stats = yearlyRepository.sumAll();
            return SettlementStatistics.fromMap(stats);
        }

        var allYearlySettlements = yearlyRepository.findByBrandId(brandId);

        Money totalSales = allYearlySettlements.stream()
            .map(SettlementYearly::getTotalSalesAmount)
            .reduce(Money.ZERO, Money::add);

        Integer totalOrderCount = allYearlySettlements.stream()
            .map(SettlementYearly::getTotalOrderCount)
            .reduce(0, Integer::sum);

        return new SettlementStatistics(totalSales, totalOrderCount);
    }

    
    private record SettlementStatistics(Money salesAmount, Integer orderCount) {

        static SettlementStatistics from(SettlementDaily daily) {
            return new SettlementStatistics(
                daily.getTotalSalesAmount(),
                daily.getTotalOrderCount()
            );
        }

        static SettlementStatistics from(SettlementWeekly weekly) {
            return new SettlementStatistics(
                weekly.getTotalSalesAmount(),
                weekly.getTotalOrderCount()
            );
        }

        static SettlementStatistics from(SettlementMonthly monthly) {
            return new SettlementStatistics(
                monthly.getTotalSalesAmount(),
                monthly.getTotalOrderCount()
            );
        }

        static SettlementStatistics from(SettlementYearly yearly) {
            return new SettlementStatistics(
                yearly.getTotalSalesAmount(),
                yearly.getTotalOrderCount()
            );
        }

        static SettlementStatistics fromMap(Map<String, Object> stats) {
            BigDecimal totalSales = (BigDecimal) stats.get("totalSalesAmount");
            Object orderCountObj = stats.get("totalOrderCount");
            int totalOrders = (orderCountObj instanceof Long)
                ? ((Long) orderCountObj).intValue()
                : (Integer) orderCountObj;

            return new SettlementStatistics(new Money(totalSales), totalOrders);
        }

        static SettlementStatistics empty() {
            return new SettlementStatistics(Money.ZERO, 0);
        }
    }

    
    private String getBrandName(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));

        return brand.getNameKo();
    }
}