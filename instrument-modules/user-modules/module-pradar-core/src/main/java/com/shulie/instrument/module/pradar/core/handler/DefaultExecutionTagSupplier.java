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
package com.shulie.instrument.module.pradar.core.handler;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.shulie.instrument.simulator.api.util.tag.ListenersUtil;
import com.shulie.instrument.simulator.message.ExecutionTagSupplier;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/10/14 4:42 下午
 */
public class DefaultExecutionTagSupplier implements ExecutionTagSupplier {
    @Override
    public int getExecutionTag(int listenerTag) {
        // 当压测已经就绪，并且当前interceptor是在压测未就绪时运行的则直接忽略
        if (PradarSwitcher.isClusterTestEnabled() && ListenersUtil.isExecuteWithClusterTestDisable(listenerTag)) {
            return ExecutionTagSupplier.EXECUTION_IGNORE;
        }
        if (PradarSwitcher.silenceSwitchOn() && !ListenersUtil.isNoSilence(listenerTag)) {
            return ExecutionTagSupplier.EXECUTION_IGNORE;
        }
        if (ListenersUtil.isFilterClusterTest(listenerTag) || (!PradarSwitcher.isSwitchSaveBusinessTrace() && ListenersUtil.isFilterBusinessData(listenerTag))) {
            if (!Pradar.isClusterTest()) {
                return ExecutionTagSupplier.EXECUTION_IGNORE;
            }
        }
        return ExecutionTagSupplier.EXECUTION_CONTINUE;
    }
}
