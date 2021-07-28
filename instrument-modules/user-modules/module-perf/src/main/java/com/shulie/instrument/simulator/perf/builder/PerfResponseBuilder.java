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
package com.shulie.instrument.simulator.perf.builder;

import com.pamirs.pradar.AppNameUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.common.RuntimeUtils;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import com.shulie.instrument.simulator.module.model.gc.GcInfo;
import com.shulie.instrument.simulator.module.model.memory.MemoryEntry;
import com.shulie.instrument.simulator.module.model.memory.MemoryInfo;
import com.shulie.instrument.simulator.module.model.thread.ThreadInfo;
import com.shulie.instrument.simulator.module.model.thread.ThreadStack;
import com.shulie.instrument.simulator.perf.entity.PerfResponse;
import com.shulie.instrument.simulator.perf.entity.ThreadVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/7 6:58 下午
 */
public class PerfResponseBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(PerfResponseBuilder.class);

    public static PerfResponse build(List<ThreadInfo> threadInfoList, GcInfo gcInfo, MemoryInfo memoryInfo) {
        PerfResponse response = new PerfResponse();
        response.setAgentId(Pradar.getAgentId());
        response.setAppIp(PradarCoreUtils.getLocalAddress());
        response.setAppName(AppNameUtils.appName());
        response.setProcessId(RuntimeUtils.getPid());
        response.setTimestamp(System.currentTimeMillis());

        if (gcInfo != null) {
            response.setFullGcCost(gcInfo.getOldGcTime());
            response.setFullGcCount(gcInfo.getOldGcCount());
            response.setYoungGcCost(gcInfo.getYoungGcTime());
            response.setYoungGcCount(gcInfo.getYoungGcCount());
        }

        if (memoryInfo != null) {
            response.setTotalMemory(memoryInfo.getHeapMemory().getTotal() + memoryInfo.getNonheapMemory().getTotal());
            long youngMemoryUsed = 0L;
            long youngMemoryTotal = 0L;
            long oldMemoryUsed = 0L;
            long oldMemoryTotal = 0L;

            //TODO 目前这块兼容的是 jdk8及以上版本,还需要兼容 jdk8以下版本和其他版本的 jdk
            for (MemoryEntry memoryEntry : memoryInfo.getHeapMemories()) {
                String poolName = beautifyName(memoryEntry.getName());
                if (poolName.equals("ps_eden_space") || poolName.equals("ps_survivor_space")
                        || poolName.equals("eden_space") || poolName.equals("survivor_space")) {
                    youngMemoryUsed = youngMemoryUsed + memoryEntry.getUsed();
                    youngMemoryTotal = youngMemoryTotal + memoryEntry.getMax();
                } else if (poolName.equals("ps_old_gen") || poolName.equals("tenured_gen")) {
                    oldMemoryUsed = oldMemoryUsed + memoryEntry.getUsed();
                    oldMemoryTotal = oldMemoryTotal + memoryEntry.getMax();
                } else {
                    LOGGER.warn("unknown poolname is {}", poolName);
                }
            }
            response.setYoungMemory(youngMemoryUsed);
            response.setTotalYoung(youngMemoryTotal);
            response.setOldMemory(oldMemoryUsed);
            response.setTotalOld(oldMemoryTotal);
            response.setTotalNonHeapMemory(memoryInfo.getNonheapMemory() == null ? 0L : memoryInfo.getNonheapMemory().getTotal());

            long totalBufferPoolMemory = 0L;
            for (MemoryEntry entry : memoryInfo.getBufferPoolMemories()) {
                totalBufferPoolMemory += entry.getTotal();
            }
            response.setTotalBufferPoolMemory(totalBufferPoolMemory);

            long permMemoryUsed = 0L;
            long permMemoryTotal = 0L;
            for (MemoryEntry memoryEntry : memoryInfo.getNonheapMemories()) {
                String poolName = beautifyName(memoryEntry.getName());
                //TODO 目前这块兼容的是 jdk8及以上版本,还需要兼容 jdk8以下版本和其他版本的 jdk
                if (StringUtils.equalsIgnoreCase(poolName, "metaspace")) {
                    permMemoryTotal = memoryEntry.getMax();
                    permMemoryUsed = memoryEntry.getUsed();
                }
            }
            response.setPermMemory(permMemoryUsed);
            response.setTotalPerm(permMemoryTotal);
            response.setHeapMemory(memoryInfo.getHeapMemory());
            response.setHeapMemories(memoryInfo.getHeapMemories());
            response.setNonheapMemory(memoryInfo.getNonheapMemory());
            response.setNonheapMemories(memoryInfo.getNonheapMemories());
            response.setBufferPoolMemories(memoryInfo.getBufferPoolMemories());
        }

        if (CollectionUtils.isNotEmpty(threadInfoList)) {
            List<ThreadVO> threadVOS = new ArrayList<ThreadVO>(threadInfoList.size());
            for (ThreadInfo threadInfo : threadInfoList) {
                ThreadVO threadVO = new ThreadVO();
                threadVO.setThreadId(threadInfo.getId());
                threadVO.setThreadName(threadInfo.getName());
                threadVO.setGroupName(threadInfo.getGroupName());
                threadVO.setThreadCpuUsage(threadInfo.getCpuUsage());
                threadVO.setCpuTime(threadInfo.getCpuTime());
                threadVO.setInterrupted(threadInfo.isInterrupted());
                threadVO.setThreadStatus(threadInfo.getState());
                threadVO.setLockName(threadInfo.getLockName());
                threadVO.setLockOwnerName(threadInfo.getLockOwnerName());
                threadVO.setLockOwnerId(threadInfo.getLockOwnerId());
                threadVO.setSuspended(threadInfo.isSuspended());
                threadVO.setInNative(threadInfo.isInNative());
                threadVO.setDaemon(threadInfo.isDaemon());
                threadVO.setPriority(threadInfo.getPriority());
                threadVO.setBlockedTime(threadInfo.getBlockedTime());
                threadVO.setBlockedCount(threadInfo.getBlockedCount());
                threadVO.setWaitedCount(threadInfo.getWaitedCount());
                threadVO.setWaitedTime(threadInfo.getWaitedTime());
                threadVO.setThreadStack(getThreadStackInfo(threadInfo.getThreadStacks()));
                threadVO.setLockIdentityHashCode(threadInfo.getLockIdentityHashCode());
                threadVO.setBlockingThreadCount(threadInfo.getBlockingThreadCount());
                threadVO.setTraceId(threadInfo.getTraceId());
                threadVO.setRpcId(threadInfo.getRpcId());
                threadVO.setClusterTest(threadInfo.isClusterTest());
                threadVOS.add(threadVO);
            }
            response.setThreadDataList(threadVOS);
        }

        return response;
    }

    private static String getThreadStackInfo(List<ThreadStack> stacks) {
        StringBuilder builder = new StringBuilder();
        if (CollectionUtils.isEmpty(stacks)) {
            return "";
        }
        for (ThreadStack stack : stacks) {
            builder.append(stack.toString()).append('\n');
        }
        return builder.toString();
    }

    private static String beautifyName(String name) {
        return name.replace(' ', '_').toLowerCase();
    }
}
