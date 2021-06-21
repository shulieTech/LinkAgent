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
package com.pamirs.pradar.interceptor;

import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 对实例方法参数包装拦截修改
 */
public abstract class ParametersWrapperInterceptorAdaptor extends ParametersWrapperInterceptor {
    protected final static Logger LOGGER = LoggerFactory.getLogger(ParametersWrapperInterceptorAdaptor.class.getName());

    /**
     * 拦截修改参数
     */
    @Override
    public final Object[] getParameter(Advice advice) throws Throwable {
        return getParameter0(advice);
    }

    protected Object[] getParameter0(Advice advice) throws Throwable {
        return advice.getParameterArray();
    }
}
