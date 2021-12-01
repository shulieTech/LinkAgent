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
package com.shulie.instrument.simulator.api;

import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * 流程控制实体类
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/23 10:45 下午
 */
public final class ProcessControlEntity {
    /**
     * 立即返回
     */
    public final static int RETURN_IMMEDIATELY = 1;

    /**
     * 立即抛出异常
     */
    public final static int THROWS_IMMEDIATELY = 2;

    /**
     * 不干预任何流程
     */
    public final static int NONE_IMMEDIATELY = 3;

    /**
     * 流程控制状态
     */
    private final int state;

    /**
     * 回应结果对象(直接返回或者抛出异常)
     */
    private final Object result;

    /**
     * 是否忽略流程的后续事件
     */
    private final boolean isIgnoreProcessEvent;

    private final static ProcessControlEntity NONE = new ProcessControlEntity(NONE_IMMEDIATELY, null);

    ProcessControlEntity(int state, Object result) {
        this(false, state, result);
    }

    ProcessControlEntity(boolean isIgnoreProcessEvent, int state, Object result) {
        this.isIgnoreProcessEvent = isIgnoreProcessEvent;
        this.state = state;
        this.result = result;
    }

    public static ProcessControlEntity none() {
        return NONE;
    }

    /**
     * 中断当前代码处理流程,并立即返回指定对象
     *
     * @param object 返回对象
     * @throws ProcessControlEntity 抛出立即返回流程控制异常
     */
    public static ProcessControlEntity returnImmediately(final Object object) {
        return new ProcessControlEntity(RETURN_IMMEDIATELY, object);
    }

    /**
     * 中断当前代码处理流程,并抛出指定异常
     *
     * @param throwable 指定异常
     * @throws ProcessControlEntity 抛出立即抛出异常流程控制异常
     */
    static ProcessControlEntity throwThrowsImmediately(final Throwable throwable) {
        return new ProcessControlEntity(THROWS_IMMEDIATELY, throwable);
    }

    public static ProcessControlEntity fromAdvice(Advice advice) {
        return (advice == null || advice.getProcessControlEntity() == null) ? NONE : advice.getProcessControlEntity();
    }

    /**
     * 判断是否需要主动忽略处理后续所有事件流
     *
     * @return 是否需要主动忽略处理后续所有事件流
     */
    public boolean isIgnoreProcessEvent() {
        return isIgnoreProcessEvent;
    }

    public int getState() {
        return state;
    }

    public Object getResult() {
        return result;
    }

}
