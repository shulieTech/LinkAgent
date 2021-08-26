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
package com.shulie.instrument.simulator.module.model.trace2;

import java.io.Serializable;

import com.shulie.instrument.simulator.api.ThrowableUtils;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/4 10:06 下午
 */
public class TraceView implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 线程名称
     */
    private String threadName;

    /**
     * 线程 ID
     */
    private long threadId;

    /**
     * 是否是守护线程
     */
    private boolean daemon;

    /**
     * 优先级
     */
    private int priority;

    /**
     * classloader
     */
    private String classloader;

    /**
     * 根节点
     */
    private TraceNode root;

    /**
     * 当前节点
     */
    private transient TraceNode current;

    /**
     * 开始时间,单位纳秒
     */
    private long traceTime;

    /**
     * 总耗时,单位纳秒
     */
    private long totalCost;

    private String args;

    private String result;

    /**
     * traceId
     */
    private String traceId;
    /**
     * rpcId
     */
    private String rpcId;
    /**
     * 方法执行前的压测流量标
     */
    private boolean isClusterTestBefore;

    /**
     * 方法执行后的压测流量标
     */
    private boolean isClusterTestAfter;

    public TraceView(String className, String methodName, String classloader) {
        root = new TraceNode(className, methodName, classloader, StackEvent.METHOD_CALL);
        root.begin();
        this.current = root;
        this.classloader = classloader;
        this.traceTime = root.getStart();
    }

    public TraceNode begin(String className, String methodName, String classloader, StackEvent stackEvent) {
        TraceNode traceNode = this.current.next(className, methodName, stackEvent);
        traceNode.begin();
        traceNode.setClassloader(classloader);
        this.current = traceNode;
        return traceNode;
    }

    public String getTraceId() {
        return traceId;
    }

    public boolean currentIsRoot() {
        return this.current == this.root;
    }

    public StackEvent lastStage() {
        return this.current.peekLastStage();
    }

    public void appendMethodCall() {
        this.current.appendMethodCall();
    }

    public TraceNode end() {
        this.current.pop();
        TraceNode traceNode = this.current;
        if (this.current.isStackEmpty()) {
            traceNode.end();
            /**
             * 如果当前节点不是根节点，则直接将当前节点设置为父节点
             */
            if (this.current != this.root) {
                this.current = this.current.getParent();
            }
        }
        return traceNode;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getClassloader() {
        return classloader;
    }

    public void setClassloader(String classloader) {
        this.classloader = classloader;
    }

    public void setTraceTime(long traceTime) {
        this.traceTime = traceTime;
    }

    public void setTotalCost(long totalCost) {
        this.totalCost = totalCost;
    }

    public long getTotalCost() {
        return totalCost;
    }

    public long getRootCost() {
        return root.getCost();
    }

    public void error(Throwable throwable) {
        this.current.setErrorMsg(ThrowableUtils.toString(throwable));
    }

    public void error(String errorMsg) {
        this.current.setErrorMsg(errorMsg);
    }

    public void setInterface(boolean isInterface) {
        this.current.setInterface(isInterface);
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
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

    public long getTraceTime() {
        return traceTime;
    }

    public TraceNode getRoot() {
        return root;
    }

    public void setRoot(TraceNode root) {
        this.root = root;
    }

    public TraceNode getCurrent() {
        return current;
    }

    public void setCurrent(TraceNode current) {
        this.current = current;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRpcId() {
        return rpcId;
    }

    public void setRpcId(String rpcId) {
        this.rpcId = rpcId;
    }

    public boolean isClusterTestBefore() {
        return isClusterTestBefore;
    }

    public void setClusterTestBefore(boolean clusterTestBefore) {
        isClusterTestBefore = clusterTestBefore;
    }

    public boolean isClusterTestAfter() {
        return isClusterTestAfter;
    }

    public void setClusterTestAfter(boolean clusterTestAfter) {
        isClusterTestAfter = clusterTestAfter;
    }

    @Override
    public String toString() {
        return "{" +
            "threadName='" + threadName + '\'' +
            ", threadId=" + threadId +
            ", daemon=" + daemon +
            ", priority=" + priority +
            ", root=" + root +
            '}';
    }

}
