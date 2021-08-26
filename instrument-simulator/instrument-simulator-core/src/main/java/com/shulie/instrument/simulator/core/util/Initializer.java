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
package com.shulie.instrument.simulator.core.util;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 初始化工具
 * 线程安全
 */
public class Initializer {
    /**
     * 初始状态(未初始化)
     */
    private final static int NEW = 1;

    /**
     * 初始化完成
     */
    private final static int INITIALIZED = 2;

    /**
     * 销毁完成
     */
    private final static int DESTROYED = 4;


    // 是否循环状态
    private final boolean isCycleState;
    // 读写锁
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);
    // 初始化状态
    private volatile int state = NEW;

    /**
     * 构造初始化器(默认为非循环状态)
     */
    public Initializer() {
        this(false);
    }

    /**
     * 构造初始化器
     *
     * @param isCycleState 是否循环状态
     */
    public Initializer(boolean isCycleState) {
        this.isCycleState = isCycleState;
    }

    /**
     * 判断是否未初始化
     *
     * @return 是否未初始化
     */
    public final boolean isNew() {
        return getState() == NEW;
    }

    /**
     * 判断是否已初始化完成
     *
     * @return 是否已初始化完成
     */
    public final boolean isInitialized() {
        return getState() == INITIALIZED;
    }

    /**
     * 判断是否已被销毁
     *
     * @return 是否已被销毁
     */
    public final boolean isDestroyed() {
        return getState() == DESTROYED;
    }

    /**
     * 获取当前状态
     *
     * @return 当前状态
     */
    public final int getState() {
        rwLock.readLock().lock();
        try {
            return state;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * 初始化过程
     *
     * @param processor 过程回调
     * @throws Throwable 初始化异常
     */
    public final void initProcess(final Processor processor) throws Throwable {
        rwLock.writeLock().lock();
        try {

            if (NEW != state) {
                throw new IllegalStateException("init process fail, because state != NEW");
            }

            processor.process();
            state = INITIALIZED;

        } finally {
            rwLock.writeLock().unlock();
        }
    }


    /**
     * 销毁过程
     *
     * @param processor 规程回调
     * @throws Throwable 销毁异常
     */
    public final void destroyProcess(final Processor processor) throws Throwable {
        rwLock.writeLock().lock();
        try {

            if (INITIALIZED != state) {
                throw new IllegalStateException("destroy process fail, because state != INITIALIZED");
            }

            processor.process();
            state = isCycleState
                    ? NEW
                    : DESTROYED
            ;

        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * 处理过程器
     */
    public interface Processor {

        /**
         * 处理过程
         *
         * @throws Throwable 处理失败
         */
        void process() throws Throwable;
    }

}
