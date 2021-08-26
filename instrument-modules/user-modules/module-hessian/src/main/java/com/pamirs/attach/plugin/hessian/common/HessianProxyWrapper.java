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
package com.pamirs.attach.plugin.hessian.common;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianProxy;
import com.caucho.hessian.client.HessianProxyFactory;
import com.pamirs.attach.plugin.hessian.HessianConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.InterceptorInvokerHelper;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/22 9:10 下午
 */
public class HessianProxyWrapper extends HessianProxy {
    private final static Logger LOGGER = LoggerFactory.getLogger(HessianProxyWrapper.class.getName());
    private Method method;
    private final DynamicFieldManager manager;

    public HessianProxyWrapper(URL url, HessianProxyFactory factory, DynamicFieldManager manager) {
        super(url, factory);
        this.manager = manager;
    }

    public HessianProxyWrapper(URL url, HessianProxyFactory factory, Class<?> type, DynamicFieldManager manager) {
        super(url, factory, type);
        this.manager = manager;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public SpanRecord beforeTrace(Object target, String methodName, Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Class<?> type = getType(target, method, args);
        if (type == null) {
            return null;
        }

        manager.setDynamicField(target, HessianConstants.DYNAMIC_FIELD_METHOD, method);
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(type.getName());
        spanRecord.setMethod(method.getName());
        spanRecord.setRequest(args);
        URL url = null;
        try {
            url = Reflect.on(target).get(HessianConstants.DYNAMIC_FIELD_URL);
        } catch (ReflectException e) {
        }
        if (url != null) {
            String host = url.getHost();
            int port = url.getPort();
            if (port == 80 || port == -1) {
                host = host + ":" + port;
            }
            spanRecord.setRemoteIp(host);
        }
        return spanRecord;
    }

    private Class<?> getType(Object target, Method method, Object[] args) {
        Class type = null;
        try {
            type = Reflect.on(target).get(HessianConstants.DYNAMIC_FIELD_URL);
        } catch (ReflectException e) {
            type = method.getDeclaringClass();
        }
        return type;
    }

    public SpanRecord afterTrace(Object target, String methodName, Method method, Object[] args, Object result) {
        if (args == null || args.length == 0) {
            return null;
        }
        Class<?> type = getType(target, method, args);
        if (type == null) {
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(type.getName());
        spanRecord.setMethod(method.getName());
        spanRecord.setRequest(args);
        spanRecord.setResponse(result);


        URL url = null;
        try {
            url = Reflect.on(target).get(HessianConstants.DYNAMIC_FIELD_URL);
        } catch (ReflectException e) {
        }
        if (url != null) {
            String host = url.getHost();
            int port = url.getPort();
            if (port == 80 || port == -1) {
                host = host + ":" + port;
            }
            spanRecord.setRemoteIp(host);
        }
        return spanRecord;
    }

    public SpanRecord exceptionTrace(Object target, String methodName, Method method, Object[] args, Throwable throwable) {
        if (args == null || args.length == 0) {
            return null;
        }
        Class<?> type = getType(target, method, args);
        if (type == null) {
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(type.getName());
        spanRecord.setMethod(method.getName());
        spanRecord.setRequest(args);
        spanRecord.setResponse(throwable);
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        URL url = null;
        try {
            url = Reflect.on(target).get(HessianConstants.DYNAMIC_FIELD_URL);
        } catch (ReflectException e) {
        }
        if (url != null) {
            String host = url.getHost();
            int port = url.getPort();
            if (port == 80 || port == -1) {
                host = host + ":" + port;
            }
            spanRecord.setRemoteIp(host);
        }
        return spanRecord;
    }

    private void clientSend(Object target, String methodName, Method method, Object[] args) {
        SpanRecord record = beforeTrace(target, methodName, method, args);
        if (record == null) {
            return;
        }
        Pradar.startClientInvoke(record.getService(), record.getMethod());
        if (record.getRequest() != null) {
            Pradar.request(record.getRequest());
        } else if (record.getRequestSize() != 0) {
            Pradar.requestSize(record.getRequestSize());
        }
        if (StringUtils.isNotBlank(record.getRemoteIp())) {
            Pradar.remoteIp(record.getRemoteIp());
        }

        if (StringUtils.isNotBlank(record.getUpAppName())) {
            Pradar.upAppName(record.getUpAppName());
        }
        if (StringUtils.isNotBlank(record.getMiddlewareName())) {
            Pradar.middlewareName(record.getMiddlewareName());
        } else {
            Pradar.middlewareName(HessianConstants.PLUGIN_NAME);
        }
    }

    private void clientRecv(Object target, String methodName, Method method, Object[] args, Object result) {
        SpanRecord record = afterTrace(target, methodName, method, args, result);
        if (record == null) {
            return;
        }
        if (record.getResponseSize() != 0) {
            Pradar.responseSize(record.getResponseSize());
        }
        Object response = record.getResponse();
        if (!Pradar.isResponseOn()) {
            response = null;
        }
        if (StringUtils.isNotBlank(record.getRemoteIp())) {
            Pradar.remoteIp(record.getRemoteIp());
        }
        Pradar.response(response);
        Pradar.endClientInvoke(record.getResultCode(), getPluginType());
    }

    private final void clientException(Object target, String methodName, Method method, Object[] args, Throwable throwable) {
        SpanRecord record = exceptionTrace(target, methodName, method, args, throwable);
        if (record == null) {
            return;
        }
        Object response = record.getResponse();
        if (!Pradar.isExceptionOn()) {
            response = null;
        }
        if (StringUtils.isNotBlank(record.getRemoteIp())) {
            Pradar.remoteIp(record.getRemoteIp());
        }
        Pradar.response(response);
        Pradar.endClientInvoke(record.getResultCode(), getPluginType());
    }

    public String getPluginName() {
        return HessianConstants.PLUGIN_NAME;
    }

    public int getPluginType() {
        return HessianConstants.PLUGIN_TYPE;
    }

    private static boolean isSkip(String methodName) {
        /**
         * 过滤掉所有的object方法
         */
        return "equals".equals(methodName)
                || "hashCode".equals(methodName)
                || "toString".equals(methodName)
                || "getClass".equals(methodName)
                || "notify".equals(methodName)
                || "notifyAll".equals(methodName)
                || "wait".equals(methodName)
                || "finalize".equals(methodName);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!isSkip(method.getName())) {
            try {
                clientSend(this, method.getName(), method, args);
            } catch (PressureMeasureError e) {
                throw e;
            } catch (Throwable e) {
                InterceptorInvokerHelper.handleException(e);
            }
        }
        try {
            Object result = super.invoke(proxy, method, args);
            if (!isSkip(method.getName())) {
                try {
                    clientRecv(this, method.getName(), method, args, result);
                } catch (PressureMeasureError e) {
                    throw e;
                } catch (Throwable e) {
                    InterceptorInvokerHelper.handleException(e);
                }
            }
            return result;
        } catch (Throwable e) {
            if (!isSkip(method.getName())) {
                try {
                    clientException(this, method.getName(), method, args, e);
                } catch (PressureMeasureError ex) {
                    throw ex;
                } catch (Throwable ex) {
                    InterceptorInvokerHelper.handleException(ex);
                }
            }

            throw e;
        }
    }

    @Override
    protected void addRequestHeaders(HessianConnection connection) {
        super.addRequestHeaders(connection);
        Map<String, String> context = Pradar.getInvokeContextTransformMap();
        for (Map.Entry<String, String> entry : context.entrySet()) {
            connection.addHeader(entry.getKey(), entry.getValue());
        }
        if (method != null) {
            connection.addHeader(HessianConstants.METHOD_HEADER, method.getName());
        }
    }
}
