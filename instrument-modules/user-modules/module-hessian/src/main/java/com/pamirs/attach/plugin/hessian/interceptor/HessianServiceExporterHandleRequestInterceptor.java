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
package com.pamirs.attach.plugin.hessian.interceptor;

import com.pamirs.attach.plugin.hessian.HessianConstants;
import com.pamirs.attach.plugin.hessian.common.WrapperRequest;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.remoting.caucho.HessianServiceExporter;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/16 9:56 上午
 */
public class HessianServiceExporterHandleRequestInterceptor extends TraceInterceptorAdaptor {
    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public String getPluginName() {
        return HessianConstants.PLUGIN_NAME;
    }

    @Override
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
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        if (args == null || args.length == 0) {
            return null;
        }
        if (!(args[0] instanceof HttpServletRequest)) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError("hessian servlet trace err! can't cast to HttpServletRequest");
            }
            return null;
        }

        HttpServletRequest request = (HttpServletRequest) args[0];
        if (!request.getMethod().equals("POST") && !request.getMethod().equals("post")) {
            return null;
        }

        String method = request.getHeader(HessianConstants.METHOD_HEADER);
        if (method != null && isSkip(method)) {
            return null;
        }
        HessianServiceExporter serviceExporter = (HessianServiceExporter) target;
        Class<?> clazz = serviceExporter.getServiceInterface();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(clazz.getName());
        spanRecord.setMethod(method);
        return spanRecord;

    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        if (args == null || args.length == 0) {
            return null;
        }
        if (!(args[0] instanceof HttpServletRequest)) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError("hessian servlet trace err! can't cast to HttpServletRequest");
            }
            return null;
        }

        HttpServletRequest request = (WrapperRequest) args[0];
        if (!request.getMethod().equals("POST") && !request.getMethod().equals("post")) {
            return null;
        }

        String method = request.getHeader(HessianConstants.METHOD_HEADER);
        if (method == null) {
            method = Pradar.getMethod();
        }
        if (method != null && isSkip(method)) {
            return null;
        }


        SpanRecord spanRecord = new SpanRecord();
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }
        if (!(args[0] instanceof HttpServletRequest)) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError("hessian servlet trace err! can't cast to HttpServletRequest");
            }
            return null;
        }

        HttpServletRequest request = (WrapperRequest) args[0];
        if (!request.getMethod().equals("POST") && !request.getMethod().equals("post")) {
            return null;
        }

        String method = request.getHeader(HessianConstants.METHOD_HEADER);
        if (method == null) {
            method = Pradar.getMethod();
        }
        if (method != null && isSkip(method)) {
            return null;
        }

        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
