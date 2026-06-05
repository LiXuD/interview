package com.interviewcoach.common.util;

import java.util.List;

/**
 * 集合工具类，提供空安全的集合操作。
 */
public final class CollectionUtils {

    private CollectionUtils() {}

    /**
     * 返回列表的不可变副本；如果输入为 null 则返回空列表。
     *
     * @param values 原始列表，可为 null
     * @return 不可变副本或空列表
     */
    public static <T> List<T> copyList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
