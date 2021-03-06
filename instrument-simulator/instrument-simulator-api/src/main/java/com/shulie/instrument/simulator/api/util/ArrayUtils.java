/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.api.util;

/**
 * 数组操作工具类
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/23 10:45 下午
 */
public class ArrayUtils {
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * 判断数组是否为空
     *
     * @param array 数组
     * @param <T>   数组类型
     * @return TRUE:数组为空(null或length==0);FALSE:数组不为空
     */
    public static <T> boolean isEmpty(T[] array) {
        return null == array
                || array.length == 0;
    }

    /**
     * 判断数组是否不为空
     *
     * @param array 数组
     * @param <T>   数组类型
     * @return TRUE:数组不为空;FALSE:数组为空(null或length==0)
     */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    /**
     * 获取数组长度
     *
     * @param array 数组
     * @param <T>   数组类型
     * @return 数组长度(null为0)
     */
    public static <T> int getLength(T[] array) {
        return isNotEmpty(array)
                ? array.length
                : 0;
    }

    /**
     *
     * @param array
     * @return
     */
    public static int[] toPrimitive(Integer[] array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].intValue();
        }
        return result;
    }

    public static String join(int[] array) {
        if (array == null) {
            return null;
        }
        if (array.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0, len = array.length; i < len; i++) {
            builder.append(array[i]).append(',');
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }
}
