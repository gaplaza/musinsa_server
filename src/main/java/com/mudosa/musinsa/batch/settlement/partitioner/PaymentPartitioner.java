package com.mudosa.musinsa.batch.settlement.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class PaymentPartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();

        String sql = """
            SELECT MIN(p.payment_id) AS minId, MAX(p.payment_id) AS maxId, COUNT(*) AS totalCount
            FROM payment p
            WHERE p.settled_at IS NULL
              AND p.payment_status = 'APPROVED'
            """;

        Map<String, Object> range = jdbcTemplate.queryForMap(sql);

        Long minId = (Long) range.get("minId");
        Long maxId = (Long) range.get("maxId");
        Long totalCount = (Long) range.get("totalCount");

        if (minId == null || maxId == null || totalCount == 0) {
            log.info("[Partitioner] 처리 대상 없음 - 빈 파티션 반환");
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", 0L);
            context.putLong("maxId", 0L);
            result.put("partition0", context);
            return result;
        }

        log.info("[Partitioner] 전체 범위 - minId: {}, maxId: {}, totalCount: {}, gridSize: {}",
                minId, maxId, totalCount, gridSize);

        long rangeSize = (maxId - minId + 1) / gridSize;
        if (rangeSize == 0) {
            rangeSize = 1;  // 최소 범위 보장
        }

        long currentMin = minId;
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();

            long currentMax;
            if (i == gridSize - 1) {
                currentMax = maxId;
            } else {
                currentMax = currentMin + rangeSize - 1;
            }

            context.putLong("minId", currentMin);
            context.putLong("maxId", currentMax);

            String partitionName = "partition" + i;
            result.put(partitionName, context);

            log.info("[Partitioner] {} - 범위: {} ~ {}", partitionName, currentMin, currentMax);

            currentMin = currentMax + 1;
        }

        return result;
    }
}
