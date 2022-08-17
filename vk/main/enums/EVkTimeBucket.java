package com.aboba.vk.main.enums;

import com.aboba.exception.MismatchEnumException;

import java.util.Arrays;
import java.util.stream.Stream;

public enum EVkTimeBucket {
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
    ALL("all"),
    INTERVAL("interval");

    private final String stringName;

    EVkTimeBucket(String stringName) {
        this.stringName = stringName;
    }

    public static EVkTimeBucket of(String timeBucket) throws MismatchEnumException {
        var response = Stream.of(EVkTimeBucket.values())
                .filter(s -> s.getVkTimeBucket().equals(timeBucket))
                .findFirst();
        if (response.isPresent()) {
            return response.get();
        } else {
            throw new MismatchEnumException("EVkTimeBucket");
        }
    }

    public static boolean isEVkTimeBucket(String stringName) {
        return Arrays.stream(EVkTimeBucket.values()).anyMatch(e -> e.getVkTimeBucket().equals(stringName));
    }

    public String getVkTimeBucket() {
        return stringName;
    }
}
