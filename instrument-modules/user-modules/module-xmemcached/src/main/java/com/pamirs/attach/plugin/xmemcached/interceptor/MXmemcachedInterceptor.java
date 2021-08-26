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
package com.pamirs.attach.plugin.xmemcached.interceptor;

import com.pamirs.attach.plugin.xmemcached.utils.XmemcachedUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.Collection;

public class MXmemcachedInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return args;
        }
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return args;
        }

        String methodName = advice.getBehavior().getName();
        if ("getProtocol".equals(methodName)) {
            return args;
        }

        Collection<String> whiteList = GlobalConfig.getInstance().getCacheKeyWhiteList();

        if (args[0] instanceof String) {
            String key = (String) args[0];

            for (String white : whiteList) {
                if (key.startsWith(white)) {
                    return args;
                }
            }

            if (!methodName.contains("get") && XmemcachedUtils.get().get(methodName) != null && !key.startsWith(Pradar.CLUSTER_TEST_PREFIX)) {
                key = Pradar.CLUSTER_TEST_PREFIX + key;
                args[0] = key;
                return args;
            }
        }
        return args;
    }
}
