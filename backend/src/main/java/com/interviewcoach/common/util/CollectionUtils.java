package com.interviewcoach.common.util;

import java.util.List;

public final class CollectionUtils {

    private CollectionUtils() {}

    public static List<String> copyList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
