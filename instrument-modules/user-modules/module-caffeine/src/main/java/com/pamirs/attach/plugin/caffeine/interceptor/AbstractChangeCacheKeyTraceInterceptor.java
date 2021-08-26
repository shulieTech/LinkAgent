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

import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/03/22 3:21 下午
 */
public abstract class AbstractChangeCacheKeyTraceInterceptor extends AbstractFilterInterceptor {

    @Override
    public Object[] doGetParameter(Advice advice) {

        Object[] args = advice.getParameterArray();
        int keyIndex = getKeyIndex(args);
        Object key = advice.getParameterArray()[keyIndex];
        if (key instanceof ClusterTestCacheWrapperKey) {
            return args;
        }

        args[keyIndex] = new ClusterTestCacheWrapperKey(args[0]);
        return args;
    }

    protected abstract int getKeyIndex(Object[] parameterArray);

}
