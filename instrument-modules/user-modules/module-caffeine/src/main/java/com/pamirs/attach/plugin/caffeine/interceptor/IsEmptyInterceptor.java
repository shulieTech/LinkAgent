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
package com.pamirs.attach.plugin.caffeine.interceptor;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 5:25 下午
 */
@SuppressWarnings("rawtypes")
public class IsEmptyInterceptor extends ResultInterceptorAdaptor {

    @Override
    protected Object getResult0(Advice advice) {
        Object returnObj = advice.getReturnObj();
        if (!(returnObj instanceof Boolean)) {
            return returnObj;
        }
        Boolean result = (Boolean)returnObj;
        if (result) {
            return returnObj;
        }
        ConcurrentMap map = (ConcurrentMap)advice.getTarget();

        Set entries = map.keySet();
        boolean isClusterTest = Pradar.isClusterTest();
        for (Object key : entries) {
            if (isClusterTest) {
                if (key instanceof ClusterTestCacheWrapperKey) {
                    return false;
                }
            } else {
                if (!(key instanceof ClusterTestCacheWrapperKey)) {
                    return false;
                }
            }
        }
        return returnObj;
    }

}
