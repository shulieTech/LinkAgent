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

import com.caucho.hessian.io.*;
import com.caucho.hessian.server.HessianSkeleton;
import com.pamirs.attach.plugin.hessian.HessianConstants;
import com.pamirs.attach.plugin.hessian.common.WrapperRequest;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.springframework.remoting.caucho.HessianServiceExporter;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/16 9:56 上午
 */
public class HessianServiceExporterHandleRequestInterceptor extends TraceInterceptorAdaptor {
    private final static Object[] EMPTY_ARGS = new Object[]{null, null};

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

        WrapperRequest request = (WrapperRequest) args[0];
        if (!request.getMethod().equals("POST") && !request.getMethod().equals("post")) {
            return null;
        }

        String method = request.getHeader(HessianConstants.METHOD_HEADER);
        if (method != null && isSkip(method)) {
            return null;
        }
        HessianServiceExporter serviceExporter = (HessianServiceExporter) target;
        HessianSkeleton hessianSkeleton = null;

        try {
            hessianSkeleton = Reflect.on(serviceExporter).get(HessianConstants.DYNAMIC_FIELD_OBJECT_SKELETON);
        } catch (ReflectException e) {
        }

        SerializerFactory serializerFactory = null;
        try {
            serializerFactory = Reflect.on(serviceExporter).get(HessianConstants.DYNAMIC_FIELD_SERIALIZER_FACTORY);
        } catch (ReflectException e) {
        }

        Object[] result = getMethodArgs(request.getInputStream(), serializerFactory, hessianSkeleton);
        Object[] arguments = (Object[]) result[1];
        if (method == null) {
            method = (String) result[0];
        }

        Class<?> clazz = serviceExporter.getServiceInterface();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(clazz.getName());
        spanRecord.setMethod(method);
        spanRecord.setRequest(arguments);
        return spanRecord;

    }


    public Object[] getMethodArgs(InputStream is,
                                  SerializerFactory serializerFactory, HessianSkeleton hessianSkeleton) {
        try {
            InputStream isToUse = is;

            if (!isToUse.markSupported()) {
                isToUse = new BufferedInputStream(isToUse);
                isToUse.mark(1);
            }

            int code = isToUse.read();
            int major;
            int minor;

            AbstractHessianInput in;
            AbstractHessianOutput out;

            if (code == 'H') {
                // Hessian 2.0 stream
                major = isToUse.read();
                minor = isToUse.read();
                if (major != 0x02) {
                    throw new IOException("Version " + major + "." + minor + " is not understood");
                }
                in = new Hessian2Input(isToUse);
                in.readCall();
            } else if (code == 'C') {
                // Hessian 2.0 call... for some reason not handled in HessianServlet!
                isToUse.reset();
                in = new Hessian2Input(isToUse);
                in.readCall();
            } else if (code == 'c') {
                // Hessian 1.0 call
                major = isToUse.read();
                minor = isToUse.read();
                in = new HessianInput(isToUse);
            } else {
                throw new IOException("Expected 'H'/'C' (Hessian 2.0) or 'c' (Hessian 1.0) in hessian input at " + code);
            }

            in.setSerializerFactory(serializerFactory);

            in.skipOptionalCall();
            while ((in.readHeader()) != null) {
                in.readObject();
            }

            String methodName = in.readMethod();
            int argLength = in.readMethodArgLength();
            Method method = null;

            if (hessianSkeleton != null) {
                try {
                    Map map = Reflect.on(hessianSkeleton).get(HessianConstants.DYNAMIC_FIELD_METHOD_MAP);
                    method = (Method) map.get(methodName + "__" + argLength);
                } catch (ReflectException e) {
                }
            }
            if ("_hessian_getAttribute".equals(methodName)) {
                String attrName = in.readString();
                in.completeCall();
                return new Object[]{"_hessian_getAttribute", new Object[0]};
            }

            Class<?>[] args = method == null ? null : method.getParameterTypes();
            if (args == null || (argLength != args.length && argLength >= 0)) {
                return EMPTY_ARGS;
            }

            Object[] values = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                values[i] = in.readObject(args[i]);
            }
            in.completeCall();
            return new Object[]{methodName, args};
        } catch (IOException e) {
            return EMPTY_ARGS;
        }
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

        WrapperRequest request = (WrapperRequest) args[0];
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

        WrapperRequest request = (WrapperRequest) args[0];
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
