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
package com.shulie.instrument.simulator.api.resource;

/**
 * 模块加载器，负责模块的加载和卸载任务执行
 * 设计主要是防止多个线程同时加载模块时可能会产生死锁
 */
public interface ModuleLoader {
    /**
     * 执行加载
     *
     * @param runnable
     */
    void load(Runnable runnable);

    /**
     * 执行卸载
     *
     * @param runnable
     */
    void unload(Runnable runnable);
}
