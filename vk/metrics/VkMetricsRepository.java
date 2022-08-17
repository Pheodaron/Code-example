package com.aboba.vk.metrics;

import com.aboba.domain.vk.main.enums.EVkTimeBucket;
import com.aboba.domain.vk.metrics.dto.VkMetricsResponse;
import com.aboba.exception.CustomControllerAdvice;
import com.aboba.exception.MismatchEnumException;
import com.aboba.utils.DateTimeParseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
public class VkMetricsRepository {
    private final Logger logger = LoggerFactory.getLogger(CustomControllerAdvice.class);

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public void addMetric(Integer id, String metricType) throws MismatchEnumException {
        if (!EVkMetric.isEVkMetric(metricType)) {
            throw new MismatchEnumException("EVkMetric");
        }
        entityManager.createNativeQuery("INSERT INTO vk_metrics (time, id, type) VALUES (NOW(), ?, ?)")
                .setParameter(1, id)
                .setParameter(2, metricType)
                .executeUpdate();
        logger.info("Register " + metricType);
    }

    public VkMetricsResponse getMetric(
            Integer id,
            EVkTimeBucket timeBucketEnum,
            EVkMetric metricType,
            String from,
            String to
    ) {
        var pair = getTimeRange(timeBucketEnum, from, to);

        Query query = entityManager.createNativeQuery("""
                        SELECT vm.type as type, count(*) as count
                        FROM vk_metrics vm
                        WHERE time > '%s' AND time < '%s' AND vm.id= ? AND type = ?
                        GROUP BY vm.type, vm.id;
                        """.formatted(pair.getFirst(), pair.getSecond()), "VkMetrics")
                .setParameter(1, id)
                .setParameter(2, metricType.getMetricType());

        @SuppressWarnings("unchecked")
        List<VkMetricsResponse> response = query.getResultList();
        if (response.size() == 0) {
            return new VkMetricsResponse(metricType.getMetricType(), 0L);
        }
        return response.get(0);
    }

    private Pair<String, String> getTimeRange(EVkTimeBucket timeBucketEnum, String from, String to) {
        var now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        var timePair = switch (timeBucketEnum) {
            case WEEK -> Pair.of(now.minusWeeks(1), now);
            case MONTH -> Pair.of(now.minusMonths(1), now);
            case YEAR -> Pair.of(now.minusYears(1), now);
            case ALL -> Pair.of(now.minusYears(10), now);
            case INTERVAL -> Pair.of(
                    DateTimeParseHelper.fromStringToLocalDateTime(from),
                    DateTimeParseHelper.fromStringToLocalDateTime(to));
        };

        return Pair.of(
                DateTimeParseHelper.toPostgreSqlFormat(timePair.getFirst()),
                DateTimeParseHelper.toPostgreSqlFormat(timePair.getSecond())
        );
    }
}
