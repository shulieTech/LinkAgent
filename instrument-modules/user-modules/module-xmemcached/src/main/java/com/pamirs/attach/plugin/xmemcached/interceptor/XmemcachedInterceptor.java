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
package com.pamirs.attach.plugin.xmemcached.interceptor;

import com.pamirs.attach.plugin.xmemcached.XmemcachedConstants;
import com.pamirs.attach.plugin.xmemcached.utils.XmemcachedUtils;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import net.rubyeye.xmemcached.MemcachedClientBuilder;

import java.lang.reflect.Method;

/**
 * @author vincent
 * @version v0.1 2016/12/29 11:10
 */
public class XmemcachedInterceptor extends TraceInterceptorAdaptor {

    private Method getDBMethod;

    public XmemcachedInterceptor() {
        try {
            getDBMethod = MemcachedClientBuilder.class.getMethod("getDb");
        } catch (Throwable e) {
        }
    }

    @Override
    public String getPluginName() {
        return XmemcachedConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return XmemcachedConstants.PLUGIN_TYPE;
    }

    private Object[] toArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        Object[] ret = new Object[args.length];
        for (int i = 0, len = args.length; i < len; i++) {
            Object arg = args[i];
            if (arg instanceof byte[]) {
                ret[i] = new String((byte[]) arg);
            } else if (arg instanceof char[]) {
                ret[i] = new String((char[]) arg);
            } else {
                ret[i] = arg;
            }
        }
        return ret;
    }


    @Override
    public SpanRecord beforeTrace(Advice advice) {
        String methodNameExt = XmemcachedUtils.getMethodNameExt(advice.getParameterArray());
        SpanRecord record = new SpanRecord();
        record.setService(advice.getBehaviorName());
        record.setMethod(methodNameExt);
        record.setRemoteIp(null);
        record.setPort(null);
        record.setRequest(toArgs(advice.getParameterArray()));
        record.setMiddlewareName(XmemcachedConstants.MIDDLEWARE_NAME);
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
        record.setResponse(advice.getThrowable());
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setMiddlewareName(XmemcachedConstants.MIDDLEWARE_NAME);
        return record;
    }


}
