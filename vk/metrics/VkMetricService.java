package com.aboba.vk.metrics;

import com.aboba.domain.vk.main.enums.EVkTimeBucket;
import com.aboba.domain.vk.metrics.dto.VkMetricsResponse;
import com.aboba.exception.MismatchEnumException;
import org.springframework.stereotype.Service;

@Service
public class VkMetricService {
    private final VkMetricsRepository vkMetricsRepository;

    public VkMetricService(VkMetricsRepository vkMetricsRepository) {
        this.vkMetricsRepository = vkMetricsRepository;
    }

    public void registerMetrics(Integer vkId, String type) throws MismatchEnumException {
        vkMetricsRepository.addMetric(vkId, type);
    }

    public VkMetricsResponse getMetrics(Integer vkUserId, EVkTimeBucket timeBucketEnum, EVkMetric metricType,
                                        String from, String to) {
        return vkMetricsRepository.getMetric(vkUserId, timeBucketEnum, metricType, from, to);
    }

    public void createCommentMetric(Integer vkId) throws MismatchEnumException {
        this.registerMetrics(vkId, "comment");
    }

    public void createLikeMetric(Integer vkId) throws MismatchEnumException {
        this.registerMetrics(vkId, "like");
    }

    public void createFriendMetric(Integer vkId) throws MismatchEnumException {
        this.registerMetrics(vkId, "friend");
    }
}
