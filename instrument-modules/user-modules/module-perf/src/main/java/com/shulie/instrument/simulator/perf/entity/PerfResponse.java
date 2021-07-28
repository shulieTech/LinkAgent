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
package com.shulie.instrument.simulator.perf.entity;

import com.shulie.instrument.simulator.module.model.memory.MemoryEntry;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/7 6:47 下午
 */
public class PerfResponse implements Serializable {
    private final static long serialVersionUID = 1L;
    /**
     * agentId
     */
    private String agentId;

    /**
     * appIp
     */
    private String appIp;

    /**
     * appName
     */
    private String appName;

    /**
     * 进程 id
     */
    private int processId;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * old memory used
     */
    private long oldMemory;

    /**
     * total old memory
     */
    private long totalOld;

    /**
     * perm memory used
     */
    private long permMemory;

    /**
     * total perm memory
     */
    private long totalPerm;

    /**
     * young memory used
     */
    private long youngMemory;

    /**
     * total young memory
     */
    private long totalYoung;

    /**
     * 非堆内存大小
     */
    private long totalNonHeapMemory;

    /**
     * buffer pool 总内存大小
     */
    private long totalBufferPoolMemory;

    /**
     * 总内存大小
     */
    private long totalMemory;

    /**
     * full gc 耗时
     */
    private long fullGcCost;

    /**
     * full gc 次数
     */
    private long fullGcCount;

    /**
     * young gc 耗时
     */
    private long youngGcCost;

    /**
     * young gc 次数
     */
    private long youngGcCount;

    /**
     * 线程信息
     */
    private List<ThreadVO> threadDataList = Collections.EMPTY_LIST;

    /**
     * 堆内存
     */
    private MemoryEntry heapMemory;
    /**
     * 堆内存详情
     */
    private List<MemoryEntry> heapMemories;

    /**
     * 非堆内存
     */
    private MemoryEntry nonheapMemory;

    /**
     * 非堆内存
     */
    private List<MemoryEntry> nonheapMemories;

    /**
     * buffer pool 内存
     */
    private List<MemoryEntry> bufferPoolMemories;

    public long getTotalBufferPoolMemory() {
        return totalBufferPoolMemory;
    }

    public void setTotalBufferPoolMemory(long totalBufferPoolMemory) {
        this.totalBufferPoolMemory = totalBufferPoolMemory;
    }

    public long getTotalNonHeapMemory() {
        return totalNonHeapMemory;
    }

    public void setTotalNonHeapMemory(long totalNonHeapMemory) {
        this.totalNonHeapMemory = totalNonHeapMemory;
    }

    public MemoryEntry getHeapMemory() {
        return heapMemory;
    }

    public void setHeapMemory(MemoryEntry heapMemory) {
        this.heapMemory = heapMemory;
    }

    public List<MemoryEntry> getHeapMemories() {
        return heapMemories;
    }

    public void setHeapMemories(List<MemoryEntry> heapMemories) {
        this.heapMemories = heapMemories;
    }

    public MemoryEntry getNonheapMemory() {
        return nonheapMemory;
    }

    public void setNonheapMemory(MemoryEntry nonheapMemory) {
        this.nonheapMemory = nonheapMemory;
    }

    public List<MemoryEntry> getNonheapMemories() {
        return nonheapMemories;
    }

    public void setNonheapMemories(List<MemoryEntry> nonheapMemories) {
        this.nonheapMemories = nonheapMemories;
    }

    public List<MemoryEntry> getBufferPoolMemories() {
        return bufferPoolMemories;
    }

    public void setBufferPoolMemories(List<MemoryEntry> bufferPoolMemories) {
        this.bufferPoolMemories = bufferPoolMemories;
    }

    public long getTotalOld() {
        return totalOld;
    }

    public void setTotalOld(long totalOld) {
        this.totalOld = totalOld;
    }

    public long getTotalPerm() {
        return totalPerm;
    }

    public void setTotalPerm(long totalPerm) {
        this.totalPerm = totalPerm;
    }

    public long getTotalYoung() {
        return totalYoung;
    }

    public void setTotalYoung(long totalYoung) {
        this.totalYoung = totalYoung;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAppIp() {
        return appIp;
    }

    public void setAppIp(String appIp) {
        this.appIp = appIp;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(int processId) {
        this.processId = processId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getOldMemory() {
        return oldMemory;
    }

    public void setOldMemory(long oldMemory) {
        this.oldMemory = oldMemory;
    }

    public long getPermMemory() {
        return permMemory;
    }

    public void setPermMemory(long permMemory) {
        this.permMemory = permMemory;
    }

    public long getYoungMemory() {
        return youngMemory;
    }

    public void setYoungMemory(long youngMemory) {
        this.youngMemory = youngMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getFullGcCost() {
        return fullGcCost;
    }

    public void setFullGcCost(long fullGcCost) {
        this.fullGcCost = fullGcCost;
    }

    public long getFullGcCount() {
        return fullGcCount;
    }

    public void setFullGcCount(long fullGcCount) {
        this.fullGcCount = fullGcCount;
    }

    public long getYoungGcCost() {
        return youngGcCost;
    }

    public void setYoungGcCost(long youngGcCost) {
        this.youngGcCost = youngGcCost;
    }

    public long getYoungGcCount() {
        return youngGcCount;
    }

    public void setYoungGcCount(long youngGcCount) {
        this.youngGcCount = youngGcCount;
    }

    public List<ThreadVO> getThreadDataList() {
        return threadDataList;
    }

    public void setThreadDataList(List<ThreadVO> threadDataList) {
        this.threadDataList = threadDataList;
    }

    @Override
    public String toString() {
        return "{" +
                "agentId='" + agentId + '\'' +
                ", appIp='" + appIp + '\'' +
                ", appName='" + appName + '\'' +
                ", processId=" + processId +
                ", timestamp=" + timestamp +
                ", oldMemory=" + oldMemory +
                ", totalOld=" + totalOld +
                ", permMemory=" + permMemory +
                ", totalPerm=" + totalPerm +
                ", youngMemory=" + youngMemory +
                ", totalYoung=" + totalYoung +
                ", totalNonHeapMemory=" + totalNonHeapMemory +
                ", totalBufferPoolMemory=" + totalBufferPoolMemory +
                ", totalMemory=" + totalMemory +
                ", fullGcCost=" + fullGcCost +
                ", fullGcCount=" + fullGcCount +
                ", youngGcCost=" + youngGcCost +
                ", youngGcCount=" + youngGcCount +
                ", threadDataList=" + threadDataList +
                ", heapMemory=" + heapMemory +
                ", heapMemories=" + heapMemories +
                ", nonheapMemory=" + nonheapMemory +
                ", nonheapMemories=" + nonheapMemories +
                ", bufferPoolMemories=" + bufferPoolMemories +
                '}';
    }
}
