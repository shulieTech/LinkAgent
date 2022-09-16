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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Logback日志框架工具类
 */
public class LogbackTempUtils {
    private static final Logger logger = LoggerFactory.getLogger(LogbackTempUtils.class);

    private static ConcurrentHashMap<String, CostTime> costTimeMap = new ConcurrentHashMap<String, CostTime>();

    public static long costTimePrint(String key, long startTime) {
//        if (costTimeMap.isEmpty()) {
//            logger.info("[tcccc]start thread");
//            new Thread(()->{
//                while (true) {
//                    try {
//                        logger.info("[tcccc]=============== print cost time!");
//                        TreeMap<String, CostTime> temp = new TreeMap<>(costTimeMap);
//                        for (Map.Entry<String, CostTime> entry : temp.entrySet()) {
//                            logger.info("[tcccc]{} cost: {}, times: {}", entry.getKey(), entry.getValue().cost, entry.getValue().times);
//                        }
//                        Thread.sleep(10000);
//                    } catch (Throwable e) {
//                        logger.error("aaaaaa", e);
//                    }
//                }
//            }).start();
//        }
//        long l = System.currentTimeMillis() - startTime;
//        costTimeMap.compute(key,(a,b)->{
//            if (b == null) {
//                b = new CostTime();
//            }
//            b.times.incrementAndGet();
//            b.cost.addAndGet(l);
//            return b;
//        });
//        return System.currentTimeMillis();
        return 0;
    }

    public static class CostTime{
        AtomicLong cost = new AtomicLong();
        AtomicLong times = new AtomicLong();
    }

}
