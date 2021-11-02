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
package com.pamirs.attach.plugin.saturn.interceptor;

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.vip.saturn.job.basic.SaturnExecutionContext;

/**
 * Create by xuyh at 2020/8/17 10:57.
 */
public class SaturnJavaJobInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "saturn";
    }

    @Override
    public int getPluginType() {
        return MiddlewareType.TYPE_JOB;
    }

    @Override
    public void beforeFirst(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        Pradar.setClusterTest(false);
        if (target instanceof com.vip.saturn.job.java.SaturnJavaJob) {
            if (methodName.equals("doExecution") && args.length == 5) {
                Object shardingContextObj = args[3];
                SaturnExecutionContext context = (SaturnExecutionContext) shardingContextObj;
                String serviceName = context.getNamespace() + "-" + context.getJobName();
                String methodNameS = context.getJobConfiguration().getJobClass();
                Pradar.startTrace(null, serviceName, methodNameS);
                Pradar.middlewareName(getPluginName());
                String jobParam = context.getJobConfiguration().getJobParameter();
                if (jobParam.contains(Pradar.PRADAR_CLUSTER_TEST_HTTP_USER_AGENT_SUFFIX)) {
                    Pradar.setClusterTest(true);
                }
            }
        }
    }

    @Override
    public void afterLast(Advice advice) {
        Pradar.endTrace();
    }
}
