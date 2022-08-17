package com.aboba.vk.main.enums;

import com.aboba.exception.MismatchEnumException;

public enum ELastSeen {
    TODAY,
    YESTERDAY,
    WEEK;

    public static ELastSeen of(String lastSeen) throws MismatchEnumException {
        return switch (lastSeen) {
            case "today" -> TODAY;
            case "yesterday" -> YESTERDAY;
            case "week" -> WEEK;
            default -> throw new MismatchEnumException(lastSeen);
        };
    }
}
