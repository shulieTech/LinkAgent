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
package com.pamirs.attach.plugin.caffeine.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ModificationInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.ArrayUtils;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 5:28 下午
 */
public abstract class AbstractFilterInterceptor extends ModificationInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return advice.getParameterArray();
        }
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args)) {
            return advice.getParameterArray();
        }
        return doGetParameter(advice);
    }

    protected abstract Object[] doGetParameter(Advice advice);
}
