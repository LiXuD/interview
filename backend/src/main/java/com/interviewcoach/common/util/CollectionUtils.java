package com.interviewcoach.common.util;

import java.util.List;

public final class CollectionUtils {

    private CollectionUtils() {}

    public static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
