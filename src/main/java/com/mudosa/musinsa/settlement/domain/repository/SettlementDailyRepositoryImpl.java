package com.mudosa.musinsa.settlement.domain.repository;

import com.mudosa.musinsa.settlement.domain.model.QSettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SettlementDailyRepositoryImpl implements SettlementDailyRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QSettlementDaily qSettlementDaily = QSettlementDaily.settlementDaily;

    @Override
    public Page<SettlementDaily> findAllWithFilters(
        Long brandId,
        LocalDate startDate,
        LocalDate endDate,
        SettlementStatus status,
        String searchTerm,
        Pageable pageable
    ) {

        BooleanBuilder builder = new BooleanBuilder();

        if (brandId != null) {
            builder.and(qSettlementDaily.brandId.eq(brandId));
        }

        if (startDate != null) {
            builder.and(qSettlementDaily.settlementDate.goe(startDate));
        }

        if (endDate != null) {
            builder.and(qSettlementDaily.settlementDate.loe(endDate));
        }

        if (status != null) {
            builder.and(qSettlementDaily.settlementStatus.eq(status));
        }

        if (searchTerm != null && !searchTerm.isBlank()) {
            builder.and(qSettlementDaily.settlementNumber.containsIgnoreCase(searchTerm));
        }

        JPAQuery<SettlementDaily> query = queryFactory
            .selectFrom(qSettlementDaily)
            .where(builder)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize());

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                String property = order.getProperty();
                if ("settlementDate".equals(property)) {
                    query.orderBy(order.isAscending()
                        ? qSettlementDaily.settlementDate.asc()
                        : qSettlementDaily.settlementDate.desc());
                } else if ("id".equals(property)) {
                    query.orderBy(order.isAscending()
                        ? qSettlementDaily.id.asc()
                        : qSettlementDaily.id.desc());
                }
            });
        } else {

            query.orderBy(qSettlementDaily.settlementDate.desc());
        }

        List<SettlementDaily> content = query.fetch();

        Long total = queryFactory
            .select(qSettlementDaily.count())
            .from(qSettlementDaily)
            .where(builder)
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public Map<String, Object> sumAllBySettlementDate(LocalDate settlementDate) {
        Tuple result = queryFactory
            .select(
                qSettlementDaily.totalSalesAmount.amount.sum(),
                qSettlementDaily.totalOrderCount.sum()
            )
            .from(qSettlementDaily)
            .where(qSettlementDaily.settlementDate.eq(settlementDate))
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
