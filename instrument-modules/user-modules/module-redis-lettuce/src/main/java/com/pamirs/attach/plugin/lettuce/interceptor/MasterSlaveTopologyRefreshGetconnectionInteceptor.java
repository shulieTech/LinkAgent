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
package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.lettuce.utils.LettuceUtils;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisNodeDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * @Auther: vernon
 * @Date: 2021/10/21 18:51
 * @Description:
 */
public class MasterSlaveTopologyRefreshGetconnectionInteceptor extends AroundInterceptor {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doBefore(Advice advice) throws Throwable {
        try {
            Iterable<RedisNodeDescription> iterable = (Iterable) advice.getParameterArray()[0];

            Iterator iterator = iterable.iterator();
            while (iterator.hasNext()) {
                RedisNodeDescription next = (RedisNodeDescription) iterator.next();
                RedisURI uri = next.getUri();
                LettuceUtils.cachePressureNode(uri);
            }
        } catch (Throwable t) {
            logger.error(Throwables.getStackTraceAsString(t));
        }
    }
}
