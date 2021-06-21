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
 * 实例方法的结果修改拦截
 * Created by xiaobin on 2017/2/6.
 */
public abstract class ResultInterceptorAdaptor extends ResultInterceptor {
    protected final static Logger LOGGER = LoggerFactory.getLogger(ResultInterceptorAdaptor.class.getName());

    /**
     * 对结果进行拦截修改
     *
     * @param advice 切点对象
     * @return
     */
    @Override
    public final Object getResult(Advice advice) throws Throwable {
        return getResult0(advice);
    }

    protected Object getResult0(Advice advice) {
        return advice.getReturnObj();
    }
}
