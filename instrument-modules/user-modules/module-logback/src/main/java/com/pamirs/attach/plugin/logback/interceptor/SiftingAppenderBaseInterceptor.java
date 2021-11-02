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
package com.pamirs.attach.plugin.logback.interceptor;

import com.pamirs.attach.plugin.logback.utils.ClusterTestMarker;
import com.pamirs.attach.plugin.logback.utils.LogEventTestFlagUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/22 12:00 下午
 */
public class SiftingAppenderBaseInterceptor extends AroundInterceptor {

    private final boolean isBusinessLogOpen;

    public SiftingAppenderBaseInterceptor(boolean isBusinessLogOpen) {this.isBusinessLogOpen = isBusinessLogOpen;}

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (!isBusinessLogOpen) {
            return;
        }
        Object[] args = advice.getParameterArray();
        if (args == null || args.length != 1) {
            return;
        }
        if (LogEventTestFlagUtils.isClusterTest(args[0])) {
            ClusterTestMarker.mark(true);
        }
    }
}
