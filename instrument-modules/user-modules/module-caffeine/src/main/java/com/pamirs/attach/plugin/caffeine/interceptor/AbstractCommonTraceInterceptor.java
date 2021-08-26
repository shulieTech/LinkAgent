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

import com.pamirs.attach.plugin.caffeine.CaffeineConstants;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 4:29 下午
 */
public abstract class AbstractCommonTraceInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return CaffeineConstants.MODULE_NAME;
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_CACHE;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setMethod(advice.getBehavior().getName());
        record.setRequest(advice.getParameterArray());
        record.setClusterTest(Pradar.isClusterTest());
        record.setService(advice.getTarget().getClass().getName());
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setMethod(advice.getBehavior().getName());
        record.setResponse(advice.getReturnObj());
        record.setClusterTest(Pradar.isClusterTest());
        record.setService(advice.getTarget().getClass().getName());
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
