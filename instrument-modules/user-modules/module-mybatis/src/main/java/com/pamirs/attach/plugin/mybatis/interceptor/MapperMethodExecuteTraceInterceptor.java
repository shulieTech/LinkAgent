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
package com.pamirs.attach.plugin.mybatis.interceptor;

import com.pamirs.attach.plugin.mybatis.MybatisConstants;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.ibatis.binding.MapperMethod;

/**
 * @ClassName: MapperProxyInvokeTraceInterceptor
 * @author: wangjian
 * @Date: 2020/12/17 15:41
 * @Description:
 */
public class MapperMethodExecuteTraceInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return MybatisConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MybatisConstants.PLUGIN_TYPE;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        MapperMethod.SqlCommand command = Reflect.on(advice.getTarget()).get(MybatisConstants.DYNAMIC_FIELD_COMMAND);
        record.setService(command.getName());
        record.setMethod(command.getType().name());
        if (advice.getParameterArray().length > 1) {
            record.setRequest(advice.getParameterArray()[1]);
        }
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getReturnObj());
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }
}
