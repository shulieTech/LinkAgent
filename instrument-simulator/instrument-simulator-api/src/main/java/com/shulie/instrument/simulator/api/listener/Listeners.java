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
package com.shulie.instrument.simulator.api.listener;

import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import com.shulie.instrument.simulator.api.util.tag.TagUtil;

import java.util.Arrays;

/**
 * 监听器构建类,构建监听器时使用
 * 此类为了减少默认模块类加载器更少加载模块中的类而设计，通常此种情形存在于多业务类加载器实例的
 * 情况下，此种情况下默认模块类加载器通常只是加载 Module 的声明类，这种情况下可以最大化的减少默认
 * 模块类加载器加载模块中的类，因为这不是必须的
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/21 5:25 下午
 */
public class Listeners {

    private String className;
    private Object[] args;
    private int listenersTag;

    /**
     * 执行策略
     */
    private int executionPolicy = ExecutionPolicy.ALWAYS;

    /**
     * 作用域名称
     */
    private String scopeName;

    /**
     * scope作用域的回调
     */
    private AdviceListenerCallback adviceListenerCallback;

    /**
     * scope 作用域的回调
     */
    private EventListenerCallback eventListenerCallback;

    private Listeners() {
    }

    public static Listeners of(final Class clazz) {
        return of(clazz, null, ExecutionPolicy.ALWAYS, (AdviceListenerCallback) null, null);
    }

    public static Listeners of(final Class clazz, final Object[] args) {
        return of(clazz, null, ExecutionPolicy.ALWAYS, (AdviceListenerCallback) null, args);
    }

    public static Listeners of(final Class clazz, final String scopeName, final EventListenerCallback callback) {
        return of(clazz, scopeName, ExecutionPolicy.ALWAYS, callback, null);
    }

    public static Listeners of(final Class clazz, final String scopeName, final EventListenerCallback callback, final Object[] args) {
        return of(clazz, scopeName, ExecutionPolicy.ALWAYS, callback, args);
    }

    public static Listeners dynamicScope(final Class clazz, final EventListenerCallback callback) {
        return dynamicScope(clazz, ExecutionPolicy.ALWAYS, callback, null);
    }

    public static Listeners dynamicScope(final Class clazz, final EventListenerCallback callback, final Object[] args) {
        return dynamicScope(clazz, ExecutionPolicy.ALWAYS, callback, args);
    }

    public static Listeners of(final Class clazz, final String scopeName, final AdviceListenerCallback callback) {
        return of(clazz, scopeName, ExecutionPolicy.ALWAYS, callback, null);
    }

    public static Listeners of(final Class clazz, final String scopeName, final AdviceListenerCallback callback, final Object[] args) {
        return of(clazz, scopeName, ExecutionPolicy.ALWAYS, callback, args);
    }

    public static Listeners dynamicScope(final Class clazz, final AdviceListenerCallback callback) {
        return dynamicScope(clazz, ExecutionPolicy.ALWAYS, callback, null);
    }

    public static Listeners dynamicScope(final Class clazz, final AdviceListenerCallback callback, final Object[] args) {
        return dynamicScope(clazz, ExecutionPolicy.ALWAYS, callback, args);
    }

    public static Listeners of(final Class clazz, final String scopeName, final int policy, final EventListenerCallback callback) {
        return of(clazz, scopeName, policy, callback, null);
    }

    public static Listeners of(final Class clazz, final String scopeName, final int policy, final EventListenerCallback callback, final Object[] args) {
        Listeners listeners = new Listeners();
        listeners.setClassName(clazz.getName());
        listeners.setScopeName(scopeName);
        listeners.setArgs(args);
        listeners.setExecutionPolicy(policy);
        listeners.setEventListenerCallback(callback);
        listeners.setListenersTag(toListenerTag(clazz));
        return listeners;
    }

    public static Listeners dynamicScope(final Class clazz, final int policy, final EventListenerCallback callback) {
        return dynamicScope(clazz, policy, callback, null);
    }

    public static Listeners dynamicScope(final Class clazz, final int policy, final EventListenerCallback callback, final Object[] args) {
        Listeners listeners = new Listeners();
        listeners.setClassName(clazz.getName());
        listeners.setArgs(args);
        listeners.setExecutionPolicy(policy);
        listeners.setEventListenerCallback(callback);
        listeners.setListenersTag(toListenerTag(clazz));
        return listeners;
    }

    public static Listeners of(final Class clazz, final String scopeName, final int policy, final AdviceListenerCallback callback) {
        return of(clazz, scopeName, policy, callback, null);
    }

    public static Listeners of(final Class clazz, final String scopeName, final int policy, final AdviceListenerCallback callback, final Object[] args) {
        Listeners listeners = new Listeners();
        listeners.setClassName(clazz.getName());
        listeners.setScopeName(scopeName);
        listeners.setArgs(args);
        listeners.setExecutionPolicy(policy);
        listeners.setAdviceListenerCallback(callback);
        listeners.setListenersTag(toListenerTag(clazz));
        return listeners;
    }

    public static Listeners dynamicScope(final Class clazz, final int policy, final AdviceListenerCallback callback) {
        return dynamicScope(clazz, policy, callback, null);
    }

    public static Listeners dynamicScope(final Class clazz, final int policy, final AdviceListenerCallback callback, final Object[] args) {
        Listeners listeners = new Listeners();
        listeners.setClassName(clazz.getName());
        listeners.setArgs(args);
        listeners.setExecutionPolicy(policy);
        listeners.setAdviceListenerCallback(callback);
        listeners.setListenersTag(toListenerTag(clazz));
        return listeners;
    }


    private static int toListenerTag(Class<?> clazz) {
        ListenerBehavior listenerBehavior = getListenerBehavior(clazz);
        if (listenerBehavior == null) {
            return 0;
        }
        return TagUtil.toTag(listenerBehavior.isFilterClusterTest(),
                listenerBehavior.isFilterBusinessData(),
                listenerBehavior.isNoSilence(),
                listenerBehavior.isExecuteWithClusterTestDisable());
    }

    private static ListenerBehavior getListenerBehavior(Class<?> clazz) {
        ListenerBehavior listenerBehavior;
        if (clazz == null) {
            return null;
        }
        listenerBehavior = clazz.getAnnotation(ListenerBehavior.class);
        if (listenerBehavior == null) {
            listenerBehavior = getListenerBehavior(clazz.getSuperclass());
        }
        return listenerBehavior;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public int getListenersTag() {
        return listenersTag;
    }

    public void setListenersTag(int listenersTag) {
        this.listenersTag = listenersTag;
    }

    public AdviceListenerCallback getAdviceListenerCallback() {
        return adviceListenerCallback;
    }

    public void setAdviceListenerCallback(AdviceListenerCallback adviceListenerCallback) {
        this.adviceListenerCallback = adviceListenerCallback;
    }

    public EventListenerCallback getEventListenerCallback() {
        return eventListenerCallback;
    }

    public void setEventListenerCallback(EventListenerCallback eventListenerCallback) {
        this.eventListenerCallback = eventListenerCallback;
    }

    public int getExecutionPolicy() {
        return executionPolicy;
    }

    public void setExecutionPolicy(int executionPolicy) {
        this.executionPolicy = executionPolicy;
    }

    @Override
    public String toString() {
        return "Listeners{" +
                "className='" + className + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Listeners listeners = (Listeners) o;

        if (executionPolicy != listeners.executionPolicy) return false;
        if (className != null ? !className.equals(listeners.className) : listeners.className != null) return false;
        return scopeName != null ? scopeName.equals(listeners.scopeName) : listeners.scopeName == null;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + executionPolicy;
        result = 31 * result + (scopeName != null ? scopeName.hashCode() : 0);
        return result;
    }
}
