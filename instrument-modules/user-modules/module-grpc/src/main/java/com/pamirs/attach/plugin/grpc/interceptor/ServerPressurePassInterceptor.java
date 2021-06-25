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
package com.pamirs.attach.plugin.grpc.interceptor;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.grpc.GrpcConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import io.grpc.Metadata;

/**
 * io.grpc.stub.ServerCalls$naryServerCallHandler#startCall
 * 方法取出Client传递上来的Metadata进行染色
 *
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.grpc.interceptor
 * @Date 2020-05-14 22:26
 */
public class ServerPressurePassInterceptor extends AroundInterceptor {
    @Resource
    protected DynamicFieldManager manager;
    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        try {
            if (PradarSwitcher.isClusterTestEnabled()) {
                if (args[1] instanceof io.grpc.Metadata) {
                    io.grpc.Metadata metadata = (Metadata) args[1];
                    Metadata.Key<String> key = Metadata.Key.of("p-pradar-cluster-test",
                        Metadata.ASCII_STRING_MARSHALLER);
                    String pt = metadata.get(key);
                    if("1".equals(pt) || "true".equals(pt)){
                        Pradar.setClusterTest(true);
                        manager.setDynamicField(advice.getTarget(), GrpcConstants.DYNAMIC_FIELD_IS_CLUSTER_TEST, true);
                    }
                }
            }
        } catch (Throwable e) {
        }
    }
}
