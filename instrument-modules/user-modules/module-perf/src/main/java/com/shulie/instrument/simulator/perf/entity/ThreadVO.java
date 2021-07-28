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

import java.io.Serializable;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/1/7 6:07 下午
 */
public class ThreadVO implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 线程 id
     */
    private long threadId;

    /**
     * 线程名称
     */
    private String threadName;

    /**
     * 线程组名称
     */
    private String groupName;

    /**
     * cpu 占用率
     */
    private long threadCpuUsage;

    /**
     * cpu 占用时长
     */
    private long cpuTime;

    /**
     * 是否中断
     */
    private boolean interrupted;

    /**
     * 线程状态
     */
    private String threadStatus;

    /**
     * 锁名称
     */
    private String lockName;

    /**
     * 锁拥有者名称
     */
    private String lockOwnerName;

    /**
     * 锁拥有者 ID
     */
    private long lockOwnerId;

    /**
     * 是否是暂停状态
     */
    private boolean suspended;

    /**
     * 线程是否在 native代码执行中
     */
    private boolean inNative;

    /**
     * 是否是后台运行
     */
    private boolean daemon;

    /**
     * 优先级
     */
    private int priority;

    /**
     * 阻塞时间
     */
    private long blockedTime;

    /**
     * 阻塞次数
     */
    private long blockedCount;

    /**
     * 等待时间
     */
    private long waitedTime;

    /**
     * 等待次数
     */
    private long waitedCount;

    /**
     * 线程堆栈
     */
    private String threadStack;

    /**
     * lock identity hash code
     */
    private int lockIdentityHashCode;

    /**
     * 阻塞的线程数
     */
    private int blockingThreadCount;

    /**
     * 链路追踪的 traceId
     */
    private String traceId;

    /**
     * 链路追踪的 rpcId
     */
    private String rpcId;

    /**
     * 是否是压测流量
     */
    private boolean isClusterTest;

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public long getThreadCpuUsage() {
        return threadCpuUsage;
    }

    public void setThreadCpuUsage(long threadCpuUsage) {
        this.threadCpuUsage = threadCpuUsage;
    }

    public long getCpuTime() {
        return cpuTime;
    }

    public void setCpuTime(long cpuTime) {
        this.cpuTime = cpuTime;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public String getThreadStatus() {
        return threadStatus;
    }

    public void setThreadStatus(String threadStatus) {
        this.threadStatus = threadStatus;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public String getLockOwnerName() {
        return lockOwnerName;
    }

    public void setLockOwnerName(String lockOwnerName) {
        this.lockOwnerName = lockOwnerName;
    }

    public long getLockOwnerId() {
        return lockOwnerId;
    }

    public void setLockOwnerId(long lockOwnerId) {
        this.lockOwnerId = lockOwnerId;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isInNative() {
        return inNative;
    }

    public void setInNative(boolean inNative) {
        this.inNative = inNative;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public long getBlockedTime() {
        return blockedTime;
    }

    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public long getWaitedTime() {
        return waitedTime;
    }

    public void setWaitedTime(long waitedTime) {
        this.waitedTime = waitedTime;
    }

    public long getWaitedCount() {
        return waitedCount;
    }

    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }

    public String getThreadStack() {
        return threadStack;
    }

    public void setThreadStack(String threadStack) {
        this.threadStack = threadStack;
    }

    public int getLockIdentityHashCode() {
        return lockIdentityHashCode;
    }

    public void setLockIdentityHashCode(int lockIdentityHashCode) {
        this.lockIdentityHashCode = lockIdentityHashCode;
    }

    public int getBlockingThreadCount() {
        return blockingThreadCount;
    }

    public void setBlockingThreadCount(int blockingThreadCount) {
        this.blockingThreadCount = blockingThreadCount;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRpcId() {
        return rpcId;
    }

    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }

    public boolean isClusterTest() {
        return isClusterTest;
    }

    public void setClusterTest(boolean clusterTest) {
        isClusterTest = clusterTest;
    }

    @Override
    public String toString() {
        return "{" +
                "threadId=" + threadId +
                ", threadName='" + threadName + '\'' +
                ", groupName='" + groupName + '\'' +
                ", threadCpuUsage=" + threadCpuUsage +
                ", cpuTime=" + cpuTime +
                ", interrupted=" + interrupted +
                ", threadStatus='" + threadStatus + '\'' +
                ", lockName='" + lockName + '\'' +
                ", lockOwnerName='" + lockOwnerName + '\'' +
                ", lockOwnerId=" + lockOwnerId +
                ", suspended=" + suspended +
                ", inNative=" + inNative +
                ", daemon=" + daemon +
                ", priority=" + priority +
                ", blockedTime=" + blockedTime +
                ", blockedCount=" + blockedCount +
                ", waitedTime=" + waitedTime +
                ", waitedCount=" + waitedCount +
                ", threadStack='" + threadStack + '\'' +
                ", lockIdentityHashCode=" + lockIdentityHashCode +
                ", blockingThreadCount=" + blockingThreadCount +
                ", traceId='" + traceId + '\'' +
                ", rpcId='" + rpcId + '\'' +
                ", isClusterTest=" + isClusterTest +
                '}';
    }
}
