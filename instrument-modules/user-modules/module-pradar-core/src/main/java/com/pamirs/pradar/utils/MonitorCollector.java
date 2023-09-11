/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar.utils;

import com.pamirs.pradar.PradarSwitcher;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    private SimulatorConfig simulatorConfig;

    private MonitorCollector(SimulatorConfig simulatorConfig) {
        this.simulatorConfig = simulatorConfig;
    }

    public static MonitorCollector getInstance(SimulatorConfig simulatorConfig) {
        if (INSTANCE == null) {
            synchronized (MonitorCollector.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MonitorCollector(simulatorConfig);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 初始化 monitor 数据收集任务
     */
    public void start() {
        // 禁止打印监控日志
        boolean enableMonitor = simulatorConfig.getBooleanProperty("pradar.switcher.monitor", true);
        if (!enableMonitor || !PradarSwitcher.isMonitorEnabled()) {
            return;
        }
        int collectRate = simulatorConfig.getIntProperty("pradar.monitor.collect.rate", 5);
        boolean runningInContainer = isRunningInsideDocker();
        if (runningInContainer) {
            executeResourcesInfoCollectingTaskInsideContainer(collectRate);
        } else {
            executeResourcesInfoCollectingTaskOutsideContainer(collectRate);
        }
    }

    private void executeResourcesInfoCollectingTaskOutsideContainer(int rate) {
        future = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new OutsideContainerResourcesInfoCollector(), 5, rate, TimeUnit.SECONDS);
    }


    private void executeResourcesInfoCollectingTaskInsideContainer(int rate) {
        future = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new InsideContainerResourcesInfoCollector(), 5, rate, TimeUnit.SECONDS);
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
                if (line.contains("/docker") || line.contains("/cke")) {
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
