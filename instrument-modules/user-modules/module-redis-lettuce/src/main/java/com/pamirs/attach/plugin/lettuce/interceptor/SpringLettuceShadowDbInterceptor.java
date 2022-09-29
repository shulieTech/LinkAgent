/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.lettuce.interceptor;

import com.pamirs.attach.plugin.lettuce.utils.Version;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/9/28 11:37
 */
public class SpringLettuceShadowDbInterceptor extends ResultInterceptorAdaptor {

    @Override
    protected Object getResult0(Advice advice) {
        if (!Pradar.isClusterTest() || !GlobalConfig.getInstance().isShadowDbRedisServer() || !Version.workWithSpringLettuce) {
            return advice.getReturnObj();
        }
        Object result = advice.getReturnObj();
        if (!(result instanceof LettuceConnectionFactory)) {
            return advice.getReturnObj();
        }
        LettuceConnectionFactory lettuceConnectionFactory = (LettuceConnectionFactory) result;
        ShadowRedisConfig shadowRedisConfig =
                GlobalConfig.getInstance().getShadowRedisConfig(lettuceConnectionFactory.getHostName() + ":" + lettuceConnectionFactory.getPort());

        RedisStandaloneConfiguration standaloneConfiguration = lettuceConnectionFactory.getStandaloneConfiguration();

        String[] nodes = StringUtils.split(shadowRedisConfig.getNodes(), ',');

        RedisStandaloneConfiguration shadowStandaloneConfiguration = new RedisStandaloneConfiguration();
        shadowStandaloneConfiguration.setHostName(nodes[0].substring(0, nodes[0].indexOf(":")).trim());
        shadowStandaloneConfiguration.setPort(Integer.parseInt(nodes[0].substring(nodes[0].indexOf(":") + 1).trim()));
        shadowStandaloneConfiguration.setDatabase(shadowRedisConfig.getDatabase());
        shadowStandaloneConfiguration.setPassword(shadowRedisConfig.getPassword(null));

        LettuceConnectionFactory shadowFactory = new LettuceConnectionFactory(standaloneConfiguration,
                lettuceConnectionFactory.getClientConfiguration());

        shadowFactory.afterPropertiesSet();
        return shadowFactory;
    }
}
