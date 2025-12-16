package com.mudosa.musinsa.settlement.infrastructure;

import com.mudosa.musinsa.settlement.domain.model.SettlementDaily;
import com.mudosa.musinsa.settlement.domain.model.SettlementMonthly;
import com.mudosa.musinsa.settlement.domain.model.SettlementWeekly;
import com.mudosa.musinsa.settlement.domain.model.SettlementYearly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcSettlementBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    @Value("${settlement.batch.upsert-enabled:true}")
    private boolean upsertEnabled;

    public void batchInsertDaily(List<SettlementDaily> list) {
        String baseSql = "INSERT INTO settlements_daily (" +
            "settlement_number, brand_id, settlement_date, settlement_timezone, " +
            "total_order_count, total_sales_amount, total_commission_amount, " +
            "total_tax_amount, total_pg_fee_amount, final_settlement_amount, " +
            "settlement_status, aggregated_at, confirmed_at, completed_at, " +
            "created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        String upsertClause = " ON DUPLICATE KEY UPDATE " +
            "total_order_count = total_order_count + VALUES(total_order_count), " +
            "total_sales_amount = total_sales_amount + VALUES(total_sales_amount), " +
            "total_commission_amount = total_commission_amount + VALUES(total_commission_amount), " +
            "total_tax_amount = total_tax_amount + VALUES(total_tax_amount), " +
            "total_pg_fee_amount = total_pg_fee_amount + VALUES(total_pg_fee_amount), " +
            "final_settlement_amount = final_settlement_amount + VALUES(final_settlement_amount), " +
            "aggregated_at = VALUES(aggregated_at), " +
            "updated_at = NOW()";

        String sql = upsertEnabled ? baseSql + upsertClause : baseSql;
        log.debug("[JDBC Daily] upsert-enabled: {}, mode: ACCUMULATE (incremental)", upsertEnabled);

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SettlementDaily item = list.get(i);
                ps.setString(1, item.getSettlementNumber());
                ps.setLong(2, item.getBrandId());
                ps.setDate(3, java.sql.Date.valueOf(item.getSettlementDate()));
                ps.setString(4, item.getSettlementTimezone());
                ps.setInt(5, item.getTotalOrderCount());
                ps.setBigDecimal(6, item.getTotalSalesAmount().getAmount());
                ps.setBigDecimal(7, item.getTotalCommissionAmount().getAmount());
                ps.setBigDecimal(8, item.getTotalTaxAmount().getAmount());
                ps.setBigDecimal(9, item.getTotalPgFeeAmount().getAmount());
                ps.setBigDecimal(10, item.getFinalSettlementAmount().getAmount());
                ps.setString(11, item.getSettlementStatus().name());
                ps.setTimestamp(12, item.getAggregatedAt() != null ? Timestamp.valueOf(item.getAggregatedAt()) : null);
                ps.setTimestamp(13, item.getConfirmedAt() != null ? Timestamp.valueOf(item.getConfirmedAt()) : null);
                ps.setTimestamp(14, item.getCompletedAt() != null ? Timestamp.valueOf(item.getCompletedAt()) : null);
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
    }

    public void batchInsertWeekly(List<SettlementWeekly> list) {
        String baseSql = "INSERT INTO settlements_weekly (" +
            "settlement_number, brand_id, settlement_year, settlement_month, week_of_month, " +
            "settlement_timezone, week_start_date, week_end_date, week_day_count, " +
            "total_order_count, total_sales_amount, total_commission_amount, " +
            "total_tax_amount, total_pg_fee_amount, final_settlement_amount, " +
            "settlement_status, aggregated_at, confirmed_at, completed_at, " +
            "created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        String upsertClause = " ON DUPLICATE KEY UPDATE " +
            "total_order_count = total_order_count + VALUES(total_order_count), " +
            "total_sales_amount = total_sales_amount + VALUES(total_sales_amount), " +
            "total_commission_amount = total_commission_amount + VALUES(total_commission_amount), " +
            "total_tax_amount = total_tax_amount + VALUES(total_tax_amount), " +
            "total_pg_fee_amount = total_pg_fee_amount + VALUES(total_pg_fee_amount), " +
            "final_settlement_amount = final_settlement_amount + VALUES(final_settlement_amount), " +
            "aggregated_at = VALUES(aggregated_at), " +
            "updated_at = NOW()";

        String sql = upsertEnabled ? baseSql + upsertClause : baseSql;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SettlementWeekly item = list.get(i);
                ps.setString(1, item.getSettlementNumber());
                ps.setLong(2, item.getBrandId());
                ps.setInt(3, item.getSettlementYear());
                ps.setInt(4, item.getSettlementMonth());
                ps.setInt(5, item.getWeekOfMonth());
                ps.setString(6, item.getSettlementTimezone());
                ps.setDate(7, java.sql.Date.valueOf(item.getWeekStartDate()));
                ps.setDate(8, java.sql.Date.valueOf(item.getWeekEndDate()));
                ps.setInt(9, item.getWeekDayCount());
                ps.setInt(10, item.getTotalOrderCount());
                ps.setBigDecimal(11, item.getTotalSalesAmount().getAmount());
                ps.setBigDecimal(12, item.getTotalCommissionAmount().getAmount());
                ps.setBigDecimal(13, item.getTotalTaxAmount().getAmount());
                ps.setBigDecimal(14, item.getTotalPgFeeAmount().getAmount());
                ps.setBigDecimal(15, item.getFinalSettlementAmount().getAmount());
                ps.setString(16, item.getSettlementStatus().name());
                ps.setTimestamp(17, item.getAggregatedAt() != null ? Timestamp.valueOf(item.getAggregatedAt()) : null);
                ps.setTimestamp(18, item.getConfirmedAt() != null ? Timestamp.valueOf(item.getConfirmedAt()) : null);
                ps.setTimestamp(19, item.getCompletedAt() != null ? Timestamp.valueOf(item.getCompletedAt()) : null);
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
    }

    public void batchInsertMonthly(List<SettlementMonthly> list) {
        String baseSql = "INSERT INTO settlements_monthly (" +
            "settlement_number, brand_id, settlement_year, settlement_month, " +
            "settlement_timezone, month_start_date, month_end_date, " +
            "total_order_count, total_sales_amount, total_commission_amount, " +
            "total_tax_amount, total_pg_fee_amount, final_settlement_amount, " +
            "settlement_status, aggregated_at, confirmed_at, completed_at, " +
            "created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        String upsertClause = " ON DUPLICATE KEY UPDATE " +
            "total_order_count = total_order_count + VALUES(total_order_count), " +
            "total_sales_amount = total_sales_amount + VALUES(total_sales_amount), " +
            "total_commission_amount = total_commission_amount + VALUES(total_commission_amount), " +
            "total_tax_amount = total_tax_amount + VALUES(total_tax_amount), " +
            "total_pg_fee_amount = total_pg_fee_amount + VALUES(total_pg_fee_amount), " +
            "final_settlement_amount = final_settlement_amount + VALUES(final_settlement_amount), " +
            "aggregated_at = VALUES(aggregated_at), " +
            "updated_at = NOW()";

        String sql = upsertEnabled ? baseSql + upsertClause : baseSql;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SettlementMonthly item = list.get(i);
                ps.setString(1, item.getSettlementNumber());
                ps.setLong(2, item.getBrandId());
                ps.setInt(3, item.getSettlementYear());
                ps.setInt(4, item.getSettlementMonth());
                ps.setString(5, item.getSettlementTimezone());
                ps.setDate(6, java.sql.Date.valueOf(item.getMonthStartDate()));
                ps.setDate(7, java.sql.Date.valueOf(item.getMonthEndDate()));
                ps.setInt(8, item.getTotalOrderCount());
                ps.setBigDecimal(9, item.getTotalSalesAmount().getAmount());
                ps.setBigDecimal(10, item.getTotalCommissionAmount().getAmount());
                ps.setBigDecimal(11, item.getTotalTaxAmount().getAmount());
                ps.setBigDecimal(12, item.getTotalPgFeeAmount().getAmount());
                ps.setBigDecimal(13, item.getFinalSettlementAmount().getAmount());
                ps.setString(14, item.getSettlementStatus().name());
                ps.setTimestamp(15, item.getAggregatedAt() != null ? Timestamp.valueOf(item.getAggregatedAt()) : null);
                ps.setTimestamp(16, item.getConfirmedAt() != null ? Timestamp.valueOf(item.getConfirmedAt()) : null);
                ps.setTimestamp(17, item.getCompletedAt() != null ? Timestamp.valueOf(item.getCompletedAt()) : null);
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
    }

    public void batchInsertYearly(List<SettlementYearly> list) {
        String baseSql = "INSERT INTO settlements_yearly (" +
            "settlement_number, brand_id, settlement_year, year_start_date, year_end_date, " +
            "settlement_timezone, total_order_count, total_sales_amount, total_commission_amount, " +
            "total_tax_amount, total_pg_fee_amount, final_settlement_amount, " +
            "settlement_status, aggregated_at, confirmed_at, completed_at, " +
            "created_at, updated_at" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        String upsertClause = " ON DUPLICATE KEY UPDATE " +
            "total_order_count = total_order_count + VALUES(total_order_count), " +
            "total_sales_amount = total_sales_amount + VALUES(total_sales_amount), " +
            "total_commission_amount = total_commission_amount + VALUES(total_commission_amount), " +
            "total_tax_amount = total_tax_amount + VALUES(total_tax_amount), " +
            "total_pg_fee_amount = total_pg_fee_amount + VALUES(total_pg_fee_amount), " +
            "final_settlement_amount = final_settlement_amount + VALUES(final_settlement_amount), " +
            "aggregated_at = VALUES(aggregated_at), " +
            "updated_at = NOW()";

        String sql = upsertEnabled ? baseSql + upsertClause : baseSql;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SettlementYearly item = list.get(i);
                ps.setString(1, item.getSettlementNumber());
                ps.setLong(2, item.getBrandId());
                ps.setInt(3, item.getSettlementYear());
                ps.setDate(4, java.sql.Date.valueOf(item.getYearStartDate()));
                ps.setDate(5, java.sql.Date.valueOf(item.getYearEndDate()));
                ps.setString(6, item.getSettlementTimezone());
                ps.setInt(7, item.getTotalOrderCount());
                ps.setBigDecimal(8, item.getTotalSalesAmount().getAmount());
                ps.setBigDecimal(9, item.getTotalCommissionAmount().getAmount());
                ps.setBigDecimal(10, item.getTotalTaxAmount().getAmount());
                ps.setBigDecimal(11, item.getTotalPgFeeAmount().getAmount());
                ps.setBigDecimal(12, item.getFinalSettlementAmount().getAmount());
                ps.setString(13, item.getSettlementStatus().name());
                ps.setTimestamp(14, item.getAggregatedAt() != null ? Timestamp.valueOf(item.getAggregatedAt()) : null);
                ps.setTimestamp(15, item.getConfirmedAt() != null ? Timestamp.valueOf(item.getConfirmedAt()) : null);
                ps.setTimestamp(16, item.getCompletedAt() != null ? Timestamp.valueOf(item.getCompletedAt()) : null);
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
    }
}