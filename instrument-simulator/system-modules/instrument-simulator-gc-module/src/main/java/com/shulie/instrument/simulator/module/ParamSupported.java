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
package com.shulie.instrument.simulator.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.shulie.instrument.simulator.api.util.ArrayUtils.isEmpty;
import static java.util.Arrays.asList;

/**
 * 命令参数支撑类
 */
public class ParamSupported {

    /**
     * 转换器(字符串到指定类型的转换器)
     *
     * @param <T> 转换目标类型
     */
    public interface Converter<T> {

        /**
         * 转换字符串为目标类型
         *
         * @param string 字符串内容
         * @return 目标类型
         */
        T convert(String string);
    }

    // 转换器集合
    final static Map<Class<?>, Converter<?>> converterMap = new HashMap<Class<?>, Converter<?>>();

    static {

        // 转换为字符串
        regConverter(new Converter<String>() {
            @Override
            public String convert(String string) {
                return string;
            }
        }, String.class);

        // 转换为Long
        regConverter(new Converter<Long>() {
            @Override
            public Long convert(String string) {
                return Long.valueOf(string);
            }
        }, long.class, Long.class);

        // 转换为Double
        regConverter(new Converter<Double>() {
            @Override
            public Double convert(String string) {
                return Double.valueOf(string);
            }
        }, double.class, Double.class);

        // 转换为Integer
        regConverter(new Converter<Integer>() {
            @Override
            public Integer convert(String string) {
                return Integer.valueOf(string);
            }
        }, int.class, Integer.class);

    }

    /**
     * 注册类型转换器
     *
     * @param converter 转换器
     * @param typeArray 类型的Java类数组
     * @param <T>       类型
     */
    protected static <T> void regConverter(Converter<T> converter, Class<T>... typeArray) {
        for (final Class<T> type : typeArray) {
            converterMap.put(type, converter);
        }
    }

    protected static <T> T getParameter(final Map<String, String> param,
                                        final String name,
                                        final Converter<T> converter,
                                        final T defaultValue) {
        final String string = param.get(name);
        return (string != null && string.length() > 0)
                ? converter.convert(string)
                : defaultValue;
    }

    protected static <T> List<T> getParameters(final Map<String, String[]> param,
                                               final String name,
                                               final Converter<T> converter,
                                               final T... defaultValueArray) {
        final String[] stringArray = param.get(name);
        if (isEmpty(stringArray)) {
            return asList(defaultValueArray);
        }
        final List<T> values = new ArrayList<T>();
        for (final String string : stringArray) {
            values.add(converter.convert(string));
        }
        return values;
    }


    protected static String getParameter(final Map<String, String> param,
                                         final String name) {
        return getParameter(
                param,
                name,
                String.class,
                null
        );
    }

    protected static boolean getBooleanParameter(final Map<String, String> param,
                                                 final String name) {
        return getBooleanParameter(param, name, false);
    }

    protected static boolean getBooleanParameter(final Map<String, String> param,
                                                 final String name, boolean defaultValue) {
        String value = getParameter(param, name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.valueOf(value);
    }

    protected static int getIntParameter(final Map<String, String> param,
                                         final String name) {
        return getIntParameter(param, name, 0);
    }

    protected static int getIntParameter(final Map<String, String> param,
                                         final String name, int defaultValue) {
        String value = getParameter(param, name);
        if (!isDigits(value)) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    protected static long getLongParameter(final Map<String, String> param,
                                           final String name) {
        return getLongParameter(param, name, 0);
    }

    protected static long getLongParameter(final Map<String, String> param,
                                           final String name, long defaultValue) {
        String value = getParameter(param, name);
        if (!isDigits(value)) {
            return defaultValue;
        }
        return Long.valueOf(value);
    }

    protected static String getParameter(final Map<String, String> param,
                                         final String name,
                                         final String defaultString) {
        return getParameter(
                param,
                name,
                String.class,
                defaultString
        );
    }

    protected static <T> T getParameter(final Map<String, String> param,
                                        final String name,
                                        final Class<T> type) {
        return getParameter(
                param,
                name,
                type,
                null
        );
    }

    protected static <T> T getParameter(final Map<String, String> param,
                                        final String name,
                                        final Class<T> type,
                                        final T defaultValue) {
        return getParameter(
                param,
                name,
                (Converter<T>) converterMap.get(type),
                defaultValue
        );
    }

    protected static <T> List<T> getParameters(final Map<String, String[]> param,
                                               final String name,
                                               final Class<T> type,
                                               final T... defaultValueArray) {
        return getParameters(
                param,
                name,
                (Converter<T>) converterMap.get(type),
                defaultValueArray
        );
    }

    public static boolean isDigits(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }


}
