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
package com.pamirs.attach.plugin.saturn.interceptor;

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;

/**
 * Create by xuyh at 2020/8/17 10:57.
 */
public class SaturnJavaJobInterceptor extends TraceInterceptorAdaptor {

//    private Field namespace;
//    private Field jobName;
//    private Field jobConfiguration;
//    private Field jobClass;
//    private Field jobParameter;


    @Override
    public String getPluginName() {
        return "saturn";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_JOB;
    }

    /**
     * 为了解决有系统抛NoClassDefFoundError，所以值全部改成反射获取
     *
     * @param advice
     */
    @Override
    public void beforeFirst(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Pradar.setClusterTest(false);
        if ("doExecution".equals(methodName) && args.length == 5) {
            Object shardingContextObj = args[3];

//            initContextField(shardingContextObj);
            Object jobConfigurationObj = Reflect.on(shardingContextObj).get("jobConfiguration");
//            initConfigField(jobConfigurationObj);

            String serviceName = Reflect.on(shardingContextObj).get("namespace") + "-" + Reflect.on(shardingContextObj).get("jobName");
            String methodNameS = Reflect.on(jobConfigurationObj).get("jobClass");
            Pradar.startTrace(null, serviceName, methodNameS);
            Pradar.middlewareName(getPluginName());
            String jobParam = Reflect.on(jobConfigurationObj).get("jobParameter");
            if (jobParam.contains(Pradar.PRADAR_CLUSTER_TEST_HTTP_USER_AGENT_SUFFIX)) {
                Pradar.setClusterTest(true);
            }
        }

    }

    @Override
    public void afterLast(Advice advice) {
        Pradar.endTrace();
    }

//    private void initContextField(Object context) {
//        if (namespace != null) {
//            return;
//        }
//        try {
//            namespace = context.getClass().getDeclaredField("namespace");
//            namespace.setAccessible(true);
//        } catch (Throwable e) {
//            //ignore
//        }
//
//        if (jobName != null) {
//            return;
//        }
//        try {
//            jobName = context.getClass().getDeclaredField("jobName");
//            jobName.setAccessible(true);
//        } catch (Throwable e) {
//            //ignore
//        }
//
//        if (jobConfiguration != null) {
//            return;
//        }
//        try {
//            jobConfiguration = context.getClass().getDeclaredField("jobConfiguration");
//            jobConfiguration.setAccessible(true);
//        } catch (Throwable e) {
//            //ignore
//        }
//    }
//
//    private void initConfigField(Object jobConfig) {
//        if (jobClass != null) {
//            return;
//        }
//        try {
//            jobClass = jobConfig.getClass().getDeclaredField("jobClass");
//            jobClass.setAccessible(true);
//        } catch (Throwable e) {
//            //ignore
//        }
//
//        if (jobParameter != null) {
//            return;
//        }
//        try {
//            jobParameter = jobConfig.getClass().getDeclaredField("jobParameter");
//            jobParameter.setAccessible(true);
//        } catch (Throwable e) {
//            //ignore
//        }
//    }
}
