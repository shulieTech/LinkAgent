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

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ExecutionCall;
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerContextFilterInterceptor extends TraceInterceptorAdaptor {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerContextFilterInterceptor.class);

    @Override
    public String getPluginName() {
        return "apache-dubbo";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_RPC;
    }

    @Override
    public void beforeFirst(Advice advice) throws Exception {
        //被调用的客户端的targetClass name
        Invoker<?> invoker = (Invoker<?>) advice.getParameterArray()[0];
        final RpcInvocation invocation = (RpcInvocation) advice.getParameterArray()[1];
        Class targetClass = invoker.getInterface();
        String className = targetClass.getCanonicalName();
        RpcContext context = RpcContext.getContext();
        String methodName = getMethodName(invocation, context);
        MatchConfig config = ClusterTestUtils.rpcClusterTest(className, methodName);
        invocation.setAttachment(PradarService.PRADAR_WHITE_LIST_CHECK, String.valueOf(config.isSuccess()));
        config.addArgs("args", advice.getParameterArray());
        config.addArgs("url", className.concat("#").concat(methodName));
        config.addArgs("isInterface", Boolean.TRUE);
        config.addArgs("class", className);
        config.addArgs("method", methodName);
        config.getStrategy().processBlock(advice.getClassLoader(), config, new ExecutionCall() {
            @Override
            public Object call(Object param) {
                try {
                    //for 2.8.4
                    return Reflect.on("org.apache.dubbo.rpc.RpcResult").create(param).get();
                } catch (Exception e) {
                    logger.info("find dubbo 2.8.4 class org.apache.dubbo.rpc.RpcResult fail, find others!", e);
                    //
                }
                try {
                    Reflect reflect = Reflect.on("org.apache.dubbo.rpc.AsyncRpcResult");
                    try {
                        //for 2.7.5
                        java.util.concurrent.CompletableFuture<AppResponse> future = new java.util.concurrent.CompletableFuture<AppResponse>();
                        future.complete(new AppResponse(param));
                        return reflect.create(future, invocation);
                    } catch (ReflectException e) {
                        //for 2.7.3
                        return reflect.create(invocation);
                    }
                } catch (Exception e) {
                    logger.error("fail to load dubbo 2.7.x class org.apache.dubbo.rpc.AsyncRpcResult", e);
                    throw new ReflectException("fail to load dubbo 2.7.x class org.apache.dubbo.rpc.AsyncRpcResult", e);
                }
            }
        });
    }

    private String getMethodName(RpcInvocation invocation, RpcContext context) {
        String methodName = invocation.getMethodName();
        if (methodName == null) {
            return context.getMethodName();
        }
        return methodName;
    }


}
