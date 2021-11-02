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
package com.pamirs.attach.plugin.redisson.interceptor;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.interceptor.ModificationInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PluginMaxRedisExpireTimeInterceptor extends ModificationInterceptorAdaptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginMaxRedisExpireTimeInterceptor.class.getName());
    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return args;
        }
        Float maxRedisExpireTime = GlobalConfig.getInstance().getMaxRedisExpireTime();
        try {
            int timeToLiveKeyIndex = getTimeToLiveKeyIndex();
            int timeUnitKeyIndex = getTimeUnitKeyIndex();
            long timeToLive = (Long)args[timeToLiveKeyIndex];
            TimeUnit timeUnit = (TimeUnit)args[timeUnitKeyIndex];
            if(maxRedisExpireTime != null && maxRedisExpireTime > -1f) {
                args[timeToLiveKeyIndex] = Math.min(timeUnit.toMillis(timeToLive),
                    Float.valueOf(maxRedisExpireTime * 60 * 60 * 1000).longValue());
                args[timeUnitKeyIndex] = TimeUnit.MILLISECONDS;
            }
        } catch (Exception exception) {
            LOGGER.error("redisson maxRedisExpireTime -method: {},new args:{},maxRedisExpireTime:{},exception:{}",advice.getBehaviorName(),
                Arrays.toString(args),maxRedisExpireTime,
                Throwables.getStackTraceAsString(exception));
        }
        return args;
    }
    protected abstract Integer getTimeToLiveKeyIndex();
    protected abstract Integer getTimeUnitKeyIndex();

}
