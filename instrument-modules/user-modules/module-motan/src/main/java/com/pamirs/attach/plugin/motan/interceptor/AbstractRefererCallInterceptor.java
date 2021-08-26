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
import com.pamirs.pradar.PradarCoreUtils;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.weibo.api.motan.rpc.AbstractReferer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;

import java.lang.reflect.Field;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/12 10:54 下午
 */
public class AbstractRefererCallInterceptor extends TraceInterceptorAdaptor {

    private Field serviceUrlField;
    private Field urlField;

    public AbstractRefererCallInterceptor() {
        try {
            this.serviceUrlField = AbstractReferer.class.getDeclaredField(MotanConstants.DYNAMIC_FIELD_SERVICE_URL);
            this.serviceUrlField.setAccessible(true);
        } catch (Throwable e) {
        }

        try {
            this.urlField = AbstractReferer.class.getDeclaredField(MotanConstants.DYNAMIC_FIELD_URL);
            this.urlField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    private URL getUrl(Object target) {
        if (serviceUrlField != null) {
            try {
                return (URL) serviceUrlField.get(target);
            } catch (Throwable e) {
            }
        }

        if (urlField != null) {
            try {
                return (URL) urlField.get(target);
            } catch (Throwable e) {
            }
        }

        try {
            return Reflect.on(target).get(MotanConstants.DYNAMIC_FIELD_SERVICE_URL);
        } catch (ReflectException e) {
            try {
                return Reflect.on(target).get(MotanConstants.DYNAMIC_FIELD_URL);
            } catch (ReflectException reflectException) {
                return null;
            }
        }
    }

    @Override
    public String getPluginName() {
        return MotanConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return MotanConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeFirst(Advice advice) {
        Object[] args = advice.getParameterArray();
        final Request request = (Request) args[0];
        String interfaceName = getInterfaceName(request);
        ClusterTestUtils.validateRpcClusterTest(interfaceName, request.getMethodName());

    }

    private String getInterfaceName(Request request) {
        return request.getInterfaceName();
    }

    private boolean isLocalHost(String address) {
        return "127.0.0.1".equals(address) || "localhost".equalsIgnoreCase(address);
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        final Request request = (Request) args[0];
        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                request.setAttachment(key, value);
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        final Request request = (Request) args[0];
        URL url = getUrl(target);
        String remoteHost = url.getHost();
        Integer port = url.getPort();
        if (isLocalHost(remoteHost)) {
            remoteHost = PradarCoreUtils.getLocalAddress();
        }

        SpanRecord record = new SpanRecord();
        record.setRemoteIp(remoteHost);
        record.setPort(port == -1 ? null : String.valueOf(port));
        String version = url.getVersion();
        version = (version != null) ? (":" + version.trim()) : "";
        final String name = request.getInterfaceName();
        record.setService(name + version);
        record.setMethod(request.getMethodName()
                + request.getParamtersDesc());
        record.setRequest(request.getArguments());
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Request req = (Request) args[0];
        Response result = (Response) advice.getReturnObj();
        SpanRecord record = new SpanRecord();
        record.setRequest(req.getArguments());
        record.setResponse(result.getValue());
        if (result.getException() != null) {
            record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
            record.setResponse(result.getException());
        }
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Request req = (Request) args[0];
        SpanRecord record = new SpanRecord();
        record.setRequest(req.getArguments());
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return record;
    }
}
