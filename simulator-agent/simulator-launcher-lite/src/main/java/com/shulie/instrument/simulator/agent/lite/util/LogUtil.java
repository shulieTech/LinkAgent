/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shulie.instrument.simulator.agent.lite.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Description 日志工具类
 * @Author ocean_wll
 * @Date 2021/12/22 10:48 上午
 */
public class LogUtil {

    /**
     * 格式化时间
     */
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    /**
     * 输出日志格式 "时间 日志级别 当前线程名 信息"
     */
    private static final String LOG_TEMPLATE = "%s %s %s %s";

    /**
     * 输出info日志
     *
     * @param info 日志信息
     */
    public static void info(String info) {
        print(LogLevelEnum.INFO, info);
    }

    /**
     * 输出warn日志
     *
     * @param info 日志信息
     */
    public static void warn(String info) {
        print(LogLevelEnum.WARN, info);
    }

    /**
     * 输出error日志
     *
     * @param info 日志信息
     */
    public static void error(String info) {
        print(LogLevelEnum.ERROR, info);
    }

    /**
     * 打印日志
     *
     * @param logLevelEnum 日志级别
     * @param info         日志信息
     */
    private static void print(LogLevelEnum logLevelEnum, String info) {
        if (info == null || info.trim().length() == 0) {
            return;
        }
        System.out.printf((LOG_TEMPLATE) + "%n", SIMPLE_DATE_FORMAT.format(new Date()),
            logLevelEnum.getVal(), "[" + Thread.currentThread().getName() + "]", info);
    }

    /**
     * 日志枚举类
     */
    private enum LogLevelEnum {
        INFO("[INFO]"),
        WARN("[WARN]"),
        ERROR("[ERROR]");

        private final String val;

        LogLevelEnum(String val) {
            this.val = val;
        }

        public String getVal() {
            return val;
        }
    }
}
