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
package com.pamirs.pradar.utils;

import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.PradarSwitcher;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.Util;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author angju
 * @date 2020/7/20 13:52
 * 服务器指标信息收集
 */
public class MonitorCollector {

    private static MonitorCollector INSTANCE;
    private ScheduledFuture future;

    private MonitorCollector() {
    }

    public static MonitorCollector getInstance() {
        if (INSTANCE == null) {
            synchronized (MonitorCollector.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MonitorCollector();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化 monitor 数据收集任务
     */
    public void start() {
        boolean runningInContainer = isRunningInsideDocker();
        if (runningInContainer) {
            executeResourcesInfoCollectingTaskInsideContainer();
        } else {
            executeResourcesInfoCollectingTaskOutsideContainer();
        }
    }

    private void executeResourcesInfoCollectingTaskOutsideContainer() {
        future = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new OutsideContainerResourcesInfoCollector(), 5, 1, TimeUnit.SECONDS);
    }


    private void executeResourcesInfoCollectingTaskInsideContainer() {
        future = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new InsideContainerResourcesInfoCollector(), 5, 1, TimeUnit.SECONDS);
    }

    private boolean isRunningInsideDocker() {
        File file = new File("/proc/1/cgroup");
        if (!file.exists()) {
            return false;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("/docker")) {
                    return true;
                }
            }
        } catch (Exception e) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {

                }
            }
        }
        return false;
    }

    public void stop() {
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }
    }
}
