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
package com.pamirs.attach.plugin.motan.interceptor;

import com.pamirs.attach.plugin.motan.MotanConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/13 9:55 上午
 */
public class ProviderMessageRouterCallInterceptor extends TraceInterceptorAdaptor {
    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public String getPluginName() {
        return MotanConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MotanConstants.PLUGIN_TYPE;
    }

    private boolean isLocalHost(String address) {
        return "127.0.0.1".equals(address) || "localhost".equalsIgnoreCase(address);
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        SpanRecord record = new SpanRecord();
        Channel channel = (Channel) args[0];
        Request request = (Request) args[1];
        String version = channel.getUrl().getVersion();
        version = (version != null) ? (":" + version.trim()) : "";

        List<String> traceKeys = Pradar.getInvokeContextTransformKeys();
        Map<String, String> ctx = new HashMap<String, String>(traceKeys.size());
        for (String traceKey : traceKeys) {
            final String value = request.getAttachments().get(traceKey);
            if (value != null) {
                ctx.put(traceKey, value);
            }
        }
        record.setContext(ctx);

        String remoteHost = null;
        int port = -1;
        if (channel.getUrl() != null) {
            remoteHost = channel.getUrl().getHost();
        }
        if (channel.getUrl() != null) {
            port = channel.getUrl().getPort();
        }
        if (isLocalHost(remoteHost)) {
            remoteHost = PradarCoreUtils.getLocalAddress();
        }

        record.setRemoteIp(remoteHost);
        record.setPort(port == -1 ? null : String.valueOf(port));
        final String interfaceName = request.getInterfaceName();
        record.setService(interfaceName + version);
        record.setMethod(request.getMethodName() + request.getParamtersDesc());
        record.setRequest(request.getArguments());
        return record;
    }

    @Override
    public void beforeLast(Advice advice) {
        ClusterTestUtils.validateClusterTest();
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        SpanRecord record = new SpanRecord();
        Request request = (Request) args[1];
        Response result = (Response) advice.getReturnObj();
        record.setRequest(request.getArguments());

        if (result == null) {
            record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        } else if (result.getException() != null) {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
            record.setResponse(result.getException());
        } else {
            record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
            record.setResponse(result.getValue());
        }
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        SpanRecord record = new SpanRecord();
        Request request = (Request) args[1];
        record.setRequest(request.getArguments());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }
}
