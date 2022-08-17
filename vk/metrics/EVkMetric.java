package com.aboba.vk.metrics;

import com.aboba.exception.MismatchEnumException;

import java.util.Arrays;
import java.util.stream.Stream;

public enum EVkMetric {
    LIKE("like"),
    COMMENT("comment"),
    FRIEND("friend"),
    MESSAGE("message");

    private final String metricType;

    EVkMetric(String metricType) {
        this.metricType = metricType;
    }

    public static EVkMetric of(String metricType) throws MismatchEnumException {
        var response = Stream.of(EVkMetric.values())
                .filter(s -> s.getMetricType().equals(metricType))
                .findFirst();
        if (response.isPresent()) {
            return response.get();
        } else {
            throw new MismatchEnumException("ETimeBucket");
        }
    }

    public static boolean isEVkMetric(String metricType) {
        return Arrays.stream(EVkMetric.values()).anyMatch(eVkMetric -> eVkMetric.getMetricType().equals(metricType));
    }

    public String getMetricType() {
        return metricType;
    }
}
