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
package com.shulie.instrument.simulator.perf;

import com.alibaba.fastjson.JSON;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.common.HttpUtils;
import com.pamirs.pradar.pressurement.base.util.PropertyUtil;
import com.shulie.instrument.simulator.api.CommandResponse;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.annotation.Command;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.resource.ModuleCommandInvoker;
import com.shulie.instrument.simulator.module.model.gc.GcInfo;
import com.shulie.instrument.simulator.module.model.memory.MemoryInfo;
import com.shulie.instrument.simulator.module.model.thread.ThreadInfo;
import com.shulie.instrument.simulator.perf.builder.PerfResponseBuilder;
import com.shulie.instrument.simulator.perf.entity.PerfResponse;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/7 5:50 下午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = PerfConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io", description = "性能模块,负责定时推送性能数据至控制台")
public class PerfPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private final static Logger logger = LoggerFactory.getLogger(PerfPlugin.class);
    private final static String PUSH_URL = "/api/agent/performance/basedata";

    @Resource
    private ModuleCommandInvoker moduleCommandInvoker;

    private ScheduledFuture future;

    /**
     * 获取 thread 信息的参数，使用公共变量
     */
    private Map<String, String> threadParams;

    private ThreadLocal<SimpleDateFormat> sdf = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHHmmss");
        }
    };

    @Override
    public boolean onActive() throws Throwable {
        boolean isPushPerfEnabled = simulatorConfig.getBooleanProperty("pradar.perf.push.enabled", true);
        logger.info("isPushPerfEnabled: {}", isPushPerfEnabled);
        if (!isPushPerfEnabled) {
            return false;
        }

        threadParams = new HashMap<String, String>();
        /**
         * 默认 thread 统计 cpu 耗时占用的时间间隔
         */
        threadParams.put("interval", "100");
        future = ExecutorServiceFactory.getFactory().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    /**
                     * 如果当前有压测流量请求，则收集
                     */
                    if (Pradar.clearHasPressureRequest()) {
                        collect();
                    }
                } catch (Throwable e) {
                    logger.error("Perf: Perf schedule collect info err! ", e);
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
        return true;
    }

    private void collect() {

        CommandResponse<List<ThreadInfo>> threadResp = moduleCommandInvoker.invokeCommand(PerfConstants.MODULE_ID_THREAD, PerfConstants.MODULE_COMMAND_THREAD_INFO, threadParams);
        CommandResponse<GcInfo> gcResp = moduleCommandInvoker.invokeCommand(PerfConstants.MODULE_ID_GC, PerfConstants.MODULE_COMMAND_GC_INFO);
        CommandResponse<MemoryInfo> memoryResp = moduleCommandInvoker.invokeCommand(PerfConstants.MODULE_ID_MEMORY, PerfConstants.MODULE_COMMAND_MEMORY_INFO);
        List<ThreadInfo> threadInfos = Collections.EMPTY_LIST;
        if (!threadResp.isSuccess()) {
            logger.error("Perf: collect perf thread info err! {}", threadResp.getMessage());
        } else {
            threadInfos = threadResp.getResult();
        }

        GcInfo gcInfo = null;
        if (!gcResp.isSuccess()) {
            logger.error("Perf: collect perf gc info err! {}", gcResp.getMessage());
        } else {
            gcInfo = gcResp.getResult();
        }

        MemoryInfo memoryInfo = null;
        if (!memoryResp.isSuccess()) {
            logger.error("Perf: collect perf memory info err! {}", memoryResp.getMessage());
        } else {
            memoryInfo = memoryResp.getResult();
        }

        PerfResponse response = PerfResponseBuilder.build(threadInfos, gcInfo, memoryInfo);
        push(response);
    }

    private void push(PerfResponse response) {
        String troControlWebUrl = PropertyUtil.getTroControlWebUrl();
        HttpUtils.HttpResult result = HttpUtils.doPost(troControlWebUrl + PUSH_URL, JSON.toJSONString(response));
        //TODO
        if (!result.isSuccess()) {
            logger.error("Perf: push perf info to tro error, status: {}, result: {}", result.getStatus(), result.getResult());
        }
    }

    @Command("info")
    public CommandResponse info(Map<String, String> args) {
        try {
            CommandResponse<List<ThreadInfo>> threadResp = moduleCommandInvoker.invokeCommand(PerfConstants.MODULE_ID_THREAD, PerfConstants.MODULE_COMMAND_THREAD_INFO, threadParams);
            CommandResponse<GcInfo> gcResp = moduleCommandInvoker.invokeCommand(PerfConstants.MODULE_ID_GC, PerfConstants.MODULE_COMMAND_GC_INFO);
            CommandResponse<MemoryInfo> memoryResp = moduleCommandInvoker.invokeCommand(PerfConstants.MODULE_ID_MEMORY, PerfConstants.MODULE_COMMAND_MEMORY_INFO);
            List<ThreadInfo> threadInfos = Collections.EMPTY_LIST;
            if (!threadResp.isSuccess()) {
                logger.error("Perf: collect perf thread info err! {}", threadResp.getMessage());
            } else {
                threadInfos = threadResp.getResult();
            }

            GcInfo gcInfo = null;
            if (!gcResp.isSuccess()) {
                logger.error("Perf: collect perf gc info err! {}", gcResp.getMessage());
            } else {
                gcInfo = gcResp.getResult();
            }

            MemoryInfo memoryInfo = null;
            if (!memoryResp.isSuccess()) {
                logger.error("Perf: collect perf memory info err! {}", memoryResp.getMessage());
            } else {
                memoryInfo = memoryResp.getResult();
            }

            PerfResponse response = PerfResponseBuilder.build(threadInfos, gcInfo, memoryInfo);
            return CommandResponse.success(response);
        } catch (Throwable e) {
            logger.error("Perf: collect perf data occurred a unknow error. ", e);
            return CommandResponse.failure(e);
        }
    }


    @Override
    public void onFrozen() throws Throwable {
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }
    }
}
