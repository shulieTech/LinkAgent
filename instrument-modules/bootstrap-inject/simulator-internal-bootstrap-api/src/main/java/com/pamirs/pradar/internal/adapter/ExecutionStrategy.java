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
package com.pamirs.pradar.internal.adapter;

import com.pamirs.pradar.internal.config.ExecutionCall;
import com.shulie.instrument.simulator.api.ProcessControlException;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.pradar.internal.adapter
 * @Date 2021/6/7 6:59 下午
 */
public interface ExecutionStrategy {

    Object processBlock(ClassLoader classLoader, Object params) throws ProcessControlException;

    Object processBlock(ClassLoader classLoader, Object params, ExecutionCall call) throws ProcessControlException;

    Object processNonBlock(ClassLoader classLoader, Object params, ExecutionCall call) throws ProcessControlException;
}
