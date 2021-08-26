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

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.HessianFactory;
import com.caucho.hessian.io.HessianInputFactory;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.hessian.server.HessianServlet;
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

import javax.servlet.http.HttpServletRequest;
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
public class HessianServletServiceInterceptor extends TraceInterceptorAdaptor {
    private HessianInputFactory _inputFactory = new HessianInputFactory();
    private HessianFactory _hessianFactory = new HessianFactory();
    private final static Object[] EMPTY_ARGS = new Object[]{null, null};

    @Override
    public String getPluginName() {
        return HessianConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return HessianConstants.PLUGIN_TYPE;
    }

    @Override
    protected boolean isClient(Advice advice) {
        return false;
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
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            return null;
        }

        String method = request.getHeader(HessianConstants.METHOD_HEADER);
        HessianServlet hessianServlet = (HessianServlet) target;
        String objectId = request.getParameter("id");
        if (objectId == null) {
            objectId = request.getParameter("ejbid");
        }
        HessianSkeleton hessianSkeleton = null;
        try {
            hessianSkeleton = Reflect.on(hessianServlet).get(HessianConstants.DYNAMIC_FIELD_OBJECT_SKELETON);
        } catch (ReflectException e) {
            try {
                hessianSkeleton = Reflect.on(hessianServlet).get(HessianConstants.DYNAMIC_FIELD_HOME_SKELETON);
            } catch (ReflectException reflectException) {
            }
        }
        Object[] result = getMethodArgs(request.getInputStream(), hessianServlet.getSerializerFactory(), hessianSkeleton);
        Object[] arguments = (Object[]) result[1];
        if (method == null) {
            method = (String) result[0];
        }

        Class<?> clazz = hessianServlet.getAPIClass();

        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setService(clazz.getName());
        spanRecord.setMethod(method);
        spanRecord.setRequest(arguments);
        return spanRecord;

    }


    public Object[] getMethodArgs(InputStream is,
                                  SerializerFactory serializerFactory, HessianSkeleton hessianSkeleton) {
        try {
            HessianInputFactory.HeaderType header = _inputFactory.readHeader(is);
            AbstractHessianInput in;
            switch (header) {
                case CALL_1_REPLY_1:
                    in = _hessianFactory.createHessianInput(is);
                    break;
                case CALL_1_REPLY_2:
                    in = _hessianFactory.createHessianInput(is);
                    break;
                case HESSIAN_2:
                    in = _hessianFactory.createHessian2Input(is);
                    in.readCall();
                    break;

                default:
                    throw new IllegalStateException(header + " is an unknown Hessian call");
            }

            if (serializerFactory != null) {
                in.setSerializerFactory(serializerFactory);
            }

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

    private Class<?> getType(Object target, Object[] args) {
        Class type = null;
        try {
            type = Reflect.on(target).get(HessianConstants.DYNAMIC_FIELD_TYPE);
        } catch (ReflectException e) {
            Method method = (Method) args[1];
            type = method.getDeclaringClass();
        }
        return type;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        if (args == null || args.length == 0) {
            return null;
        }
        Class<?> type = getType(target, args);
        if (type == null) {
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        if (args == null || args.length == 0) {
            return null;
        }
        Class<?> type = getType(target, args);
        if (type == null) {
            return null;
        }
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        return spanRecord;
    }
}
