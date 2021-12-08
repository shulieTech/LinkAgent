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
package com.pamirs.attach.plugin.log4j.interceptor.v2.async;

import com.pamirs.attach.plugin.log4j.destroy.Log4jDestroy;
import com.pamirs.attach.plugin.log4j.message.WithTestFlagMessage;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.logging.log4j.message.Message;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/08 3:22 下午
 */
@Destroyable(Log4jDestroy.class)
public class AsyncTestMarkSetInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return advice.getParameterArray();
        }
        Object[] args = advice.getParameterArray();
        if (!(args[3] instanceof Message)) {
            return args;
        }
        args[3] = new WithTestFlagMessage((Message)args[3]);
        return args;
    }
}
