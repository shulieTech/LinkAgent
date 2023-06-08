/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.feign.interceptor;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.feign.FeignConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.common.ClassAssignableUtil;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import feign.InvocationHandlerFactory;
import feign.MethodMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.feign.interceptor
 * @Date 2021/6/7 2:44 下午
 */
public class FeignMockInterceptor extends TraceInterceptorAdaptor {

    private final Logger logger = LoggerFactory.getLogger(FeignMockInterceptor.class);
    private final Logger mockLogger = LoggerFactory.getLogger("FEIGN-MOCK-LOGGER");
    private static final Gson gson = new Gson();

    private static ExecutionStrategy fixJsonStrategy =
            new JsonMockStrategy() {
                @Override
                public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {
                    if (params instanceof MatchConfig) {
                        try {
                            MatchConfig config = (MatchConfig) params;
                            String scriptContent = config.getScriptContent().trim();
                            Pradar.mockResponse(scriptContent);
                            Advice advice = (Advice) config.getArgs().get("advice");
                            Map<Method, InvocationHandlerFactory.MethodHandler>
                                    dispatch = Reflect.on(advice.getTarget()).get("dispatch");
                            InvocationHandlerFactory.MethodHandler methodHandler
                                    = dispatch.get(advice.getParameterArray()[1]);
                            MethodMetadata methodMetadata = Reflect.on(methodHandler).get("metadata");
                            Type typeRef = Reflect.on(methodMetadata).get("returnType");
                            Object result = gson.fromJson(scriptContent, typeRef);
                            ProcessController.returnImmediately(result);
                        } catch (ProcessControlException pe) {
                            throw pe;
                        } catch (Throwable t) {
                            throw new PressureMeasureError(t);
                        }
                    }
                    return null;
                }
            };


    @Override
    public void beforeFirst(Advice advice) throws ProcessControlException {
        if (Pradar.isClusterTest()) {
            Object[] parameterArray = advice.getParameterArray();
            Method method = (Method) parameterArray[1];
            String className = method.getDeclaringClass().getName();
            final String methodName = method.getName();
            //todo ClusterTestUtils.rpcClusterTest里面已经做了对象copy，这么写是为了能单模块更新，后面要去掉
            MatchConfig config = copyMatchConfig(ClusterTestUtils.rpcClusterTest(className, methodName));
            config.addArgs("args", advice.getParameterArray());
            config.addArgs("mockLogger", mockLogger);
            config.addArgs("url", className.concat("#").concat(methodName));
            config.addArgs("isInterface", Boolean.TRUE);
            config.addArgs("class", className);
            config.addArgs("method", methodName);

            if (config.getStrategy() instanceof JsonMockStrategy) {
                config.addArgs("advice", advice);
                fixJsonStrategy.processBlock(method.getReturnType(), advice.getClassLoader(), config);
            }
            config.getStrategy().processBlock(method.getReturnType(), advice.getClassLoader(), config);
        }

    }

    private static MatchConfig copyMatchConfig(MatchConfig matchConfig) {
        MatchConfig copied = new MatchConfig();
        copied.setUrl(matchConfig.getUrl());
        copied.setStrategy(matchConfig.getStrategy());
        copied.setScriptContent(matchConfig.getScriptContent());
        copied.setArgs(new HashMap<String, Object>(matchConfig.getArgs()));
        copied.setForwarding(matchConfig.getForwarding());
        copied.setSuccess(matchConfig.isSuccess());
        return copied;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Method method = (Method) args[1];
        if (method == null) {
            logger.info("[debug] feign method =null , args: [{}]", JSON.toJSONString(args));
            return null;
        }
        Object[] arg = (Object[]) args[2];
        SpanRecord record = new SpanRecord();
        record.setService(method.getDeclaringClass().getName());
        record.setMethod(method.getName() + getParameterTypesString(method.getParameterTypes()));
        // MultipartFile toJSONString时会报异常
        if (arg != null) {
            List<Object> toStringArgs = new ArrayList<Object>();
            for (Object o : arg) {
                if (ClassAssignableUtil.isInstance(o, "org.springframework.web.multipart.MultipartFile")) {
                    toStringArgs.add(ReflectionUtils.invoke(arg, "getName"));
                } else {
                    toStringArgs.add(o);
                }
            }
            record.setRequest(JSON.toJSONString(toStringArgs));
        }
        if (Pradar.isClusterTest()) {
            record.setPassedCheck(true);
        }
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Method method = (Method) args[1];
        if (method == null) {
            logger.info("[debug] thread {} feign method =null , args: [{}]", Thread.currentThread(), JSON.toJSONString(args));
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_SUCCESS);
        record.setService(method.getDeclaringClass().getName());
        record.setMethod(method.getName() + getParameterTypesString(method.getParameterTypes()));
        record.setResponse(advice.getReturnObj());
        return record;
    }

    static String getParameterTypesString(Class<?>[] classes) {
        if (classes == null || classes.length == 0) {
            return "()";
        }
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        for (Class<?> clazz : classes) {
            if (clazz == null) {
                continue;
            }
            builder.append(clazz.getSimpleName()).append(',');
        }
        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        Method method = (Method) args[1];
        if (method == null) {
            logger.info("[debug] thread {} feign method =null , args: [{}]", Thread.currentThread(), JSON.toJSONString(args));
            return null;
        }
        Object[] arg = (Object[]) args[2];
        SpanRecord record = new SpanRecord();
        record.setService(method.getDeclaringClass().getName());
        record.setMethod(method.getName() + getParameterTypesString(method.getParameterTypes()));
        record.setRequest(arg);
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

    @Override
    public String getPluginName() {
        return FeignConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return FeignConstants.PLUGIN_TYPE;
    }
}
