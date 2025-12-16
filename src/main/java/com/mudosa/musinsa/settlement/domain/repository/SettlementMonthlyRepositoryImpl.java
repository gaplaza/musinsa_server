package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.QSettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementStatus;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SettlementMonthlyRepositoryImpl implements SettlementMonthlyRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QSettlementMonthly qSettlementMonthly = QSettlementMonthly.settlementMonthly;

    @Override
    public Page<SettlementMonthly> findAllWithFilters(
        Long brandId,
        Integer year,
        Integer month,
        SettlementStatus status,
        String searchTerm,
        Pageable pageable
    ) {

        BooleanBuilder builder = new BooleanBuilder();

        if (brandId != null) {
            builder.and(qSettlementMonthly.brandId.eq(brandId));
        }

        if (year != null) {
            builder.and(qSettlementMonthly.settlementYear.eq(year));
        }

        if (month != null) {
            builder.and(qSettlementMonthly.settlementMonth.eq(month));
        }

        if (status != null) {
            builder.and(qSettlementMonthly.settlementStatus.eq(status));
        }

        if (searchTerm != null && !searchTerm.isBlank()) {
            builder.and(qSettlementMonthly.settlementNumber.containsIgnoreCase(searchTerm));
        }

        JPAQuery<SettlementMonthly> query = queryFactory
            .selectFrom(qSettlementMonthly)
            .where(builder)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                if ("settlementYear".equals(property)) {
                    query.orderBy(order.isAscending()
                        ? qSettlementMonthly.settlementYear.asc()
                        : qSettlementMonthly.settlementYear.desc());
                } else if ("settlementMonth".equals(property)) {
                    query.orderBy(order.isAscending()
                        ? qSettlementMonthly.settlementMonth.asc()
                        : qSettlementMonthly.settlementMonth.desc());
                } else if ("id".equals(property)) {
                    query.orderBy(order.isAscending()
                        ? qSettlementMonthly.id.asc()
                        : qSettlementMonthly.id.desc());
                }
            });
        } else {

            query.orderBy(
                qSettlementMonthly.settlementYear.desc(),
                qSettlementMonthly.settlementMonth.desc()
            );
        }

        List<SettlementMonthly> content = query.fetch();

        Long total = queryFactory
            .select(qSettlementMonthly.count())
            .from(qSettlementMonthly)
            .where(builder)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Map<String, Object> sumAllByYearAndMonth(int year, int month) {
        Tuple result = queryFactory
            .select(
                qSettlementMonthly.totalSalesAmount.amount.sum(),
                qSettlementMonthly.totalOrderCount.sum()
            )
            .from(qSettlementMonthly)
            .where(
                qSettlementMonthly.settlementYear.eq(year),
                qSettlementMonthly.settlementMonth.eq(month)
            )
            .fetchOne();

        Map<String, Object> stats = new HashMap<>();

        if (result != null) {
            BigDecimal totalSales = result.get(0, BigDecimal.class);
            Integer totalOrders = result.get(1, Integer.class);

            stats.put("totalSalesAmount", totalSales != null ? totalSales : BigDecimal.ZERO);
            stats.put("totalOrderCount", totalOrders != null ? totalOrders : 0);
        } else {
            stats.put("totalSalesAmount", BigDecimal.ZERO);
            stats.put("totalOrderCount", 0);
        }

        return stats;
    }
}
