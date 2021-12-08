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
package com.pamirs.attach.plugin.apache.cxf.interceptor;

import com.pamirs.attach.plugin.apache.cxf.CxfConstants;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.cxf.message.Message;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jiangjibo
 * @date 2021/11/11 5:01 下午
 * @description:
 */
public class InvokeInterceptor extends TraceInterceptorAdaptor {

    public String getPluginName() {
        return CxfConstants.PLUGIN_NAME;
    }

    public int getPluginType() {
        return CxfConstants.PLUGIN_TYPE;
    }

    public SpanRecord beforeTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setMethod(advice.getBehaviorName());
        record.setService(advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
        return record;
    }

    @Override
    protected ContextTransfer getContextTransfer(final Advice advice) {
        return new ContextTransfer() {
            public void transfer(String key, String value) {
                final Message message = (Message)advice.getParameterArray()[1];
                Map<String, List> headers = (Map)message.get(Message.PROTOCOL_HEADERS);
                if (headers == null) {
                    headers = new HashMap();
                }

                ((Map)headers).put(key, Collections.singletonList(value));
                message.put(Message.PROTOCOL_HEADERS, headers);
            }
        };
    }

    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setMethod(advice.getBehaviorName());
        record.setService(advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
        record.setResponse(advice.getReturnObj());
        return record;
    }

    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setMethod(advice.getBehaviorName());
        record.setRequest(advice.getParameterArray());
        record.setService(advice.getBehaviorName());
        record.setResponse(Throwables.getStackTraceAsString(advice.getThrowable()));
        return record;
    }
}
