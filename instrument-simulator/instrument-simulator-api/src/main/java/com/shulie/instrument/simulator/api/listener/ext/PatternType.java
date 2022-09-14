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
package com.shulie.instrument.simulator.api.listener.ext;

/**
 * 模版匹配模式
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/23 10:47 下午
 */
public abstract class PatternType {
    /**
     * 通配符表达式
     */
    public final static int WILDCARD = 1;

    /**
     * 字符串严格匹配
     */
    public final static int STRING = 3;

    /**
     * 正则表达式
     */
    public final static int REGEX = 2;

    public static int of(String name) {
        if ("REGEX".equalsIgnoreCase(name)) {
            return REGEX;
        } else if ("STRING".equalsIgnoreCase(name)) {
            return STRING;
        }
        return PatternType.WILDCARD;
    }

    public static String name(int type) {
        if (type == REGEX) {
            return "REGEX";
        }
        if (type == WILDCARD) {
            return "WILDCARD";
        }
        if (type == STRING) {
            return "STRING";
        }
        return "UNKNOW";
    }
}
