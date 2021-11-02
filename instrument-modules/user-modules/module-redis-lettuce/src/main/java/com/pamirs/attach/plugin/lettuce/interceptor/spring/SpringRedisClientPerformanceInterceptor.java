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
package com.pamirs.attach.plugin.lettuce.interceptor.spring;

import com.pamirs.attach.plugin.lettuce.utils.Version;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: vernon
 * @Date: 2021/9/2 15:27
 * @Description:
 */
public class SpringRedisClientPerformanceInterceptor extends SpringRedisClientInfoCollector {

    static Map<LettuceConnectionFactory, LettuceConnectionFactory> cache = new ConcurrentHashMap();

    private static final Logger LOGGER = LoggerFactory.getLogger(
            SpringRedisClientPerformanceInterceptor.class.getName());

    @Override
    protected Object getResult0(Advice advice) {
        if (!Pradar.isClusterTest() || !GlobalConfig.getInstance().isShadowDbRedisServer()
                || !Version.workWithSpringLettuce) {
            /**
             * 业务流量信息采集
             */
            if (!Pradar.isClusterTest()) {
                attachment(advice);
            }
            return advice.getReturnObj();
        }

        LettuceConnectionFactory factory = null;

        factory = cache.get(advice.getTarget());
        if (factory == null) {
            factory = new LettuceFactoryProxy().setBiz((LettuceConnectionFactory) advice.getTarget()).getFactory();
            if (factory != null) {
                cache.put((LettuceConnectionFactory) advice.getTarget(), factory);
            }
        }
        try {

            return Reflect.on(factory).call(advice.getBehavior().getName()).get();
        } catch (PressureMeasureError e) {
            LOGGER.error("lettuce getConnection error,{}", e);
            ;
            throw new PressureMeasureError("", e);
        } catch (Throwable e) {
            LOGGER.error("lettuce getConnection error,{}", e);
            ;
        }
        return null;
    }
}
