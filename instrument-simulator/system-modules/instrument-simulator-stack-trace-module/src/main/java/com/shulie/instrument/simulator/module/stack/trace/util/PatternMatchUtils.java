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
package com.shulie.instrument.simulator.module.stack.trace.util;

import com.shulie.instrument.simulator.api.util.StringUtil;

import static com.shulie.instrument.simulator.api.listener.ext.PatternType.REGEX;
import static com.shulie.instrument.simulator.api.listener.ext.PatternType.WILDCARD;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/08/10 3:25 下午
 */
public class PatternMatchUtils {

    /**
     * 模式匹配
     *
     * @param string      目标字符串
     * @param patterns    模式字符串
     * @param patternType 匹配模式
     * @return TRUE:匹配成功 / FALSE:匹配失败
     */
    public static boolean patternMatching(final String string,
        final String[] patterns,
        final int patternType) {
        switch (patternType) {
            case WILDCARD:
                if (patterns == null || patterns.length == 0) {
                    return false;
                }
                for (String p : patterns) {
                    boolean matches = StringUtil.matching(string, p);
                    if (matches) {
                        return true;
                    }
                }
                return false;
            case REGEX:
                if (patterns == null || patterns.length == 0) {
                    return false;
                }
                for (String p : patterns) {
                    boolean matches = string.matches(p);
                    if (matches) {
                        return true;
                    }
                }
                return false;
            default:
                return false;
        }
    }
}
