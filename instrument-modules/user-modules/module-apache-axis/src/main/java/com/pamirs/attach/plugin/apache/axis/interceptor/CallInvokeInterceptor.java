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
/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.pamirs.attach.plugin.apache.axis.interceptor;

import com.pamirs.attach.plugin.apache.axis.AxisConstants;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.axis.MessageContext;
import org.apache.axis.client.Call;

import javax.xml.soap.MimeHeaders;
import java.net.URI;

/**
 * @author jiangjibo
 * @date 2021/11/11 6:03 下午
 * @description:
 */
public class CallInvokeInterceptor extends TraceInterceptorAdaptor {

    public String getPluginName() {
        return AxisConstants.PLUGIN_NAME;
    }

    public int getPluginType() {
        return AxisConstants.PLUGIN_TYPE;
    }

    public SpanRecord beforeTrace(Advice advice) {
        Call call = (Call)advice.getTarget();
        final MessageContext messageContext = call.getMessageContext();
        String operationName = call.getOperationName().toString();
        String serviceName = call.getTargetEndpointAddress();
        SpanRecord record = new SpanRecord();
        record.setService(serviceName);
        record.setMethod(operationName);
        record.setRequest(messageContext.getMessage());

        try {
            URI uri = URI.create(serviceName);
            String host = uri.getHost();
            record.setRemoteIp(host);
            record.setPort(uri.getHost());
        } catch (Throwable var11) {
        }
        return record;
    }

    @Override
    protected ContextTransfer getContextTransfer(final Advice advice) {
        return new ContextTransfer() {
            public void transfer(String key, String value) {
                Call call = (Call)advice.getTarget();
                MimeHeaders headers = call.getMessageContext().getMessage().getMimeHeaders();
                headers.addHeader(key, value);
            }
        };
    }

    public SpanRecord afterTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getReturnObj());
        return record;
    }

    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord record = new SpanRecord();
        record.setResponse(advice.getThrowable());
        record.setResultCode("01");
        return record;
    }

}
