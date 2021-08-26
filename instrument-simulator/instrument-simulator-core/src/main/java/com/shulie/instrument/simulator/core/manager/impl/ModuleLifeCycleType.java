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
package com.shulie.instrument.simulator.core.manager.impl;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/16 3:30 下午
 */

/**
 * 模块生命周期类型
 */
public interface ModuleLifeCycleType {

    /**
     * 模块加载
     */
    int MODULE_LOAD = 1;

    /**
     * 模块卸载
     */
    int MODULE_UNLOAD = 2;

    /**
     * 模块激活
     */
    int MODULE_ACTIVE = 3;

    /**
     * 模块冻结
     */
    int MODULE_FROZEN = 4;

    /**
     * 模块加载完成
     */
    int MODULE_LOAD_COMPLETED = 5;
}
