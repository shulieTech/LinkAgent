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
package com.pamirs.attach.plugin.apache.dubbo.interceptor;

import com.google.gson.Gson;
import com.pamirs.attach.plugin.apache.dubbo.DubboConstants;
import com.pamirs.attach.plugin.apache.dubbo.utils.ClassTypeUtils;
import com.pamirs.pradar.*;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.adapter.ExecutionStrategy;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.mock.JsonMockStrategy;
import com.shulie.instrument.simulator.api.ProcessControlException;
import com.shulie.instrument.simulator.api.ProcessController;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vincent
 */
@ListenerBehavior(isFilterBusinessData = true)
public class DubboConsumerInterceptor extends TraceInterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(DubboConsumerInterceptor.class);

    public void initConsumer(Invoker<?> invoker, Invocation invocation) {
        RpcContext.getContext()
                .setInvoker(invoker)
                .setInvocation(invocation)
                .setLocalAddress(NetUtils.getLocalHost(), 0)
                .setRemoteAddress(invoker.getUrl().getHost(),
                        invoker.getUrl().getPort());
    }

    @Override
    public String getPluginName() {
        return DubboConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return DubboConstants.PLUGIN_TYPE;
    }

    private boolean isLocalHost(String address) {
        return "127.0.0.1".equals(address) || "localhost".equals(address) || "LOCALHOST".equals(address);
    }

    private boolean isMonitorService(Invoker invoker) {
        if (invoker != null) {
            return "com.alibaba.dubbo.monitor.MonitorService".equals(invoker.getInterface().getName());
        }
        return false;
    }
    //申通事件中心过滤掉
    private boolean isShentongEvent(String name){
        if (name.equals("com.sto.event.ocean.client.remote.EventAccept")
            || name.equals("com.sto.event.ocean.client.remote.EventCoreRpc")
            || name.equals("com.sto.event.ocean.client.remote.EventPreRpc")){
            return true;
        }
        return false;
    }

    private static ExecutionStrategy fixJsonStrategy =
            new JsonMockStrategy() {
                @Override
                public Object processBlock(Class returnType, ClassLoader classLoader, Object params) throws ProcessControlException {

                    MatchConfig config = (MatchConfig) params;
                    RpcInvocation invocation = (RpcInvocation) config.getArgs().get("invocation");
                    try {
                        //for 2.8.4
                        return Reflect.on("org.apache.dubbo.rpc.RpcResult").create(config.getScriptContent()).get();
                    } catch (Exception e) {
                        if (logger.isInfoEnabled()) {
                            logger.info("find dubbo 2.8.4 class org.apache.dubbo.rpc.RpcResult fail, find others!", e);
                        }
                        //
                    }
                    try {
                        Reflect reflect = Reflect.on("org.apache.dubbo.rpc.AsyncRpcResult");
                        try {
                            //for 2.7.5
                            java.util.concurrent.CompletableFuture<AppResponse> future = new java.util.concurrent.CompletableFuture<AppResponse>();
                            future.complete(new AppResponse(getResultByType(invocation.getReturnType(),config.getScriptContent())));
                            Reflect result = reflect.create(future, invocation);
                            ProcessController.returnImmediately(returnType, result.get());
                        } catch (ReflectException e) {
                            //for 2.7.3
                            Reflect result = reflect.create(invocation);
                            ProcessController.returnImmediately(returnType, result.get());
                        }
                    } catch (ProcessControlException pe) {
                        throw pe;
                    } catch (Exception e) {
                        logger.error("fail to load dubbo 2.7.x class org.apache.dubbo.rpc.AsyncRpcResult", e);
                        throw new ReflectException("fail to load dubbo 2.7.x class org.apache.dubbo.rpc.AsyncRpcResult", e);
                    }
                    return null;
                }
            };

    private static final Gson gson = new Gson();

    private static final Object getResultByType(Class classType, String result){
        try {
            String classTypeName = classType.getName();
            int code = -1;
            if (ClassTypeUtils.getType2Code().containsKey(classTypeName)){
                code = ClassTypeUtils.getType2Code().get(classTypeName);
            }
            switch (code){
                case ClassTypeUtils.INT:
                    return Integer.valueOf(result);
                case ClassTypeUtils.BOOLEAN:
                    return Boolean.valueOf(result);
                case ClassTypeUtils.FLOAT:
                    return Float.valueOf(result);
                case ClassTypeUtils.DOUBLE:
                    return Double.valueOf(result);
                case ClassTypeUtils.LONG:
                    return Long.valueOf(result);
                case ClassTypeUtils.SHORT:
                    return Short.valueOf(result);
                case ClassTypeUtils.STRING:
                    return result;
                default:
                    return gson.fromJson(result, classType);
            }
        }catch (Throwable t){
            logger.error("dubbo mock返回值类型转换异常,classType is"  + classType.getName());
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.mock)
                    .setErrorCode("mock-0003")
                    .setMessage("mock处理异常")
                    .setDetail("dubbo mock返回值类型转换异常,classType is"  + classType.getName())
                    .report();
            throw new PradarException("dubbo mock返回值类型转换异常,classType is " + classType.getName());
        }

    }

    @Override
    public void beforeLast(Advice advice) throws ProcessControlException {
        final RpcInvocation invocation = (RpcInvocation) advice.getParameterArray()[0];
        String interfaceName = getInterfaceName(invocation);
        String methodName = invocation.getMethodName();
        MatchConfig config = ClusterTestUtils.rpcClusterTest(interfaceName, methodName);
        invocation.setAttachment(PradarService.PRADAR_WHITE_LIST_CHECK, String.valueOf(config.isSuccess()));
        config.addArgs("args", advice.getParameterArray());
        config.addArgs("url", interfaceName.concat("#").concat(methodName));
        config.addArgs("isInterface", Boolean.TRUE);
        config.addArgs("class", interfaceName);
        config.addArgs("method", methodName);
        config.addArgs("invocation", invocation);
        if(isShentongEvent(interfaceName)){
            config.addArgs(PradarService.PRADAR_WHITE_LIST_CHECK, "true");
        }
        if (config.getStrategy() instanceof JsonMockStrategy){
            fixJsonStrategy.processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config);
        }
        config.getStrategy().processBlock(advice.getBehavior().getReturnType(), advice.getClassLoader(), config, new ExecutionCall() {
            @Override
            public Object call(Object param) {
                try {
                    //for 2.8.4
                    return Reflect.on("org.apache.dubbo.rpc.RpcResult").create(param).get();
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("find dubbo 2.8.4 class org.apache.dubbo.rpc.RpcResult fail, find others!", e);
                    }
                    //
                }
                try {
                    Reflect reflect = Reflect.on("org.apache.dubbo.rpc.AsyncRpcResult");
                    try {
                        //for 2.7.5
                        java.util.concurrent.CompletableFuture<AppResponse> future = new java.util.concurrent.CompletableFuture<AppResponse>();
                        future.complete(new AppResponse(param));
                        Reflect result = reflect.create(future, invocation);
                        return result.get();
                    } catch (ReflectException e) {
                        //for 2.7.3
                        Reflect result = reflect.create(invocation);
                        return result.get();
                    }
                } catch (Exception e) {
                    logger.error("fail to load dubbo 2.7.x class org.apache.dubbo.rpc.AsyncRpcResult", e);
                    throw new ReflectException("fail to load dubbo 2.7.x class org.apache.dubbo.rpc.AsyncRpcResult", e);
                }
            }
        });
    }

    private String getInterfaceName(Invocation invocation) {
        Invoker invoker = invocation.getInvoker();
        if (invoker != null) {
            return invoker.getInterface().getCanonicalName();
        } else {
            return invocation.getAttachment("interface");
        }
    }

    private String buildServiceName(String className, String version) {
        if (StringUtils.isBlank(version)) {
            return className;
        }
        return className + ':' + version;
    }

    private String getVersion(Invocation invocation) {
        Invoker invoker = invocation.getInvoker();
        if (invoker != null) {
            return invoker.getUrl().getParameter("version");
        } else {
            return invocation.getAttachment("version");
        }
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        final RpcInvocation invocation = (RpcInvocation) advice.getParameterArray()[0];
        Invoker<?> invoker = (Invoker<?>) advice.getTarget();
        if (isMonitorService(invoker)) {
            return null;
        }
        if (invocation instanceof RpcInvocation) {
            return new ContextTransfer() {
                @Override
                public void transfer(String key, String value) {
                    (invocation).setAttachment(key, value);
                }
            };
        }
        return null;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        final RpcInvocation invocation = (RpcInvocation) advice.getParameterArray()[0];
        final String name = getInterfaceName(invocation);
        if (isShentongEvent(name)){
            return null;
        }
        Invoker<?> invoker = (Invoker<?>) advice.getTarget();
        if (isMonitorService(invoker)) {
            return null;
        }

        initConsumer((Invoker<?>) advice.getTarget(), invocation);
        RpcContext context = RpcContext.getContext();
        String remoteHost = context.getRemoteHost();
        if (isLocalHost(remoteHost)) {
            remoteHost = PradarCoreUtils.getLocalAddress();
        }

        SpanRecord record = new SpanRecord();
        record.setRemoteIp(remoteHost);
        record.setPort(context.getRemotePort());
        String version = getVersion(invocation);
        record.setService(buildServiceName(name, version));
        record.setMethod(context.getMethodName()
                + DubboUtils.getParameterTypesString(context.getParameterTypes()));
        record.setRequestSize(DubboUtils.getRequestSize());
        record.setRequest(invocation.getArguments());
        record.setPassedCheck(Boolean.parseBoolean(invocation.getAttachment(PradarService.PRADAR_WHITE_LIST_CHECK)));
        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Result result = (Result) advice.getReturnObj();
        final RpcInvocation invocation = (RpcInvocation) advice.getParameterArray()[0];
        final String name = getInterfaceName(invocation);
        if (isShentongEvent(name)){
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResponse(DubboUtils.getResponse(result));
        record.setResponseSize(result == null ? 0 : DubboUtils.getResponseSize(result));
        if (result.hasException()) {
            record.setResultCode(DubboUtils.getResultCode(result.getException()));
            record.setResponse(result.getException());
        }
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        final RpcInvocation invocation = (RpcInvocation) advice.getParameterArray()[0];
        final String name = getInterfaceName(invocation);
        if (isShentongEvent(name)){
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResponseSize(0);
        record.setResponse(advice.getThrowable());
        record.setResultCode(DubboUtils.getResultCode(advice.getThrowable()));
        return record;
    }
}
