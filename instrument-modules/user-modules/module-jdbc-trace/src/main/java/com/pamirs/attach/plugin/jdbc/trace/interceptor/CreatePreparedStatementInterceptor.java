/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.jdbc.trace.interceptor;

import com.pamirs.attach.plugin.common.datasource.trace.CheckedTracePreparedStatement;
import com.pamirs.attach.plugin.common.datasource.utils.ProxyFlag;
import com.pamirs.attach.plugin.jdbc.trace.JdbcTraceConstants;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/31 14:51
 */
public class CreatePreparedStatementInterceptor extends AroundInterceptor {

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (ProxyFlag.inProxy() || advice.getReturnObj() instanceof CheckedTracePreparedStatement) {
            return;
        }
        dynamicFieldManager.setDynamicField(advice.getReturnObj(), JdbcTraceConstants.sql,
                advice.getParameterArray()[0]);
    }
}
