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
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/4 10:07 下午
 */
public class TraceNode implements Serializable {
    private final static long serialVersionUID = 1L;

    /**
     * 父节点
     */
    private transient TraceNode parent;

    /**
     * 子点节
     */
    private final List<TraceNode> children = new ArrayList<TraceNode>();

    /**
     * 类名
     */
    private String className;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 是否是接口
     */
    private boolean isInterface;

    /**
     * 开始时间戳
     */
    private long start;
    /**
     * 结束时间戳
     */
    private long end;

    /**
     * 是否成功
     */
    private boolean success = true;

    /**
     * 抛出的异常
     */
    private String errorMsg;

    /**
     * 行号
     */
    private long line = -1;

    /**
     * 耗时
     */
    private long cost;

    /**
     * 类加载器
     */
    private String classloader;

    private String args;

    private String result;

    private Stack<StackEvent> stack = new Stack<StackEvent>();

    public TraceNode(String className, String methodName, String classloader, StackEvent stage) {
        this.className = className;
        this.methodName = methodName;
        this.classloader = classloader;
        stack.push(stage);
    }

    public TraceNode(TraceNode parent, String className, String methodName, StackEvent stage) {
        this.parent = parent;
        this.className = className;
        this.methodName = methodName;
        stack.push(stage);
    }

    public TraceNode next(String className, String methodName, StackEvent stage) {
        TraceNode traceNode = new TraceNode(this, className, methodName, stage);
        this.children.add(traceNode);
        return traceNode;
    }

    public StackEvent stage() {
        return stack.peek();
    }

    public TraceNode begin() {
        this.start = System.nanoTime();
        return this;
    }

    public TraceNode end() {
        this.end = System.nanoTime();
        this.cost = (end - start) < 0 ? 0 : (end - start);
        return this;
    }

    public String getClassloader() {
        return classloader;
    }

    public void setClassloader(String classloader) {
        this.classloader = classloader;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    /**
     * 判断是否需要跳过
     *
     * @param stopInMills
     * @return
     */
    public boolean isSkip(long stopInMills) {
        return (getCost() / 1000000) <= stopInMills || isSkip();
    }

    /**
     * 是否忽略
     *
     * @return
     */
    public boolean isSkip() {
        return isJdkClass(className);
    }

    public boolean isSuccess() {
        return success;
    }

    public long getLine() {
        return line;
    }

    public void setLine(long line) {
        this.line = line;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        if (errorMsg != null) {
            this.success = true;
        }
    }

    /**
     * 是否根节点
     *
     * @return true / false
     */
    boolean isRoot() {
        return null == parent;
    }

    /**
     * 判断是否是 jdk 的类
     *
     * @param className
     * @return
     */
    private boolean isJdkClass(String className) {
        return className.startsWith("java.");
    }

    public long getCost() {
        return cost;
    }

    public TraceNode getParent() {
        return parent;
    }

    public List<TraceNode> getChildren() {
        return children;
    }

    public void setChildren(List<TraceNode> children) {
        if (children != null) {
            this.children.clear();
            this.children.addAll(children);
        }
    }

    public String getClassName() {
        return className;
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

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    public String toString() {
        return "{" +
            "className='" + className + '\'' +
            ", methodName='" + methodName + '\'' +
            ", isInterface=" + isInterface +
            ", start=" + start +
            ", end=" + end +
            ", cost=" + cost +
            ", success=" + success +
            ", errorMsg='" + errorMsg + '\'' +
            ", line=" + line +
            ", children=" + children +
            '}';
    }

    public StackEvent peekLastStage() {
        return stack.peek();
    }

    public void appendMethodCall() {
        this.stack.push(StackEvent.METHOD_CALL);
    }

    public StackEvent pop() {
        return stack.pop();
    }

    public boolean isStackEmpty() {
        return stack.isEmpty();
    }

}
