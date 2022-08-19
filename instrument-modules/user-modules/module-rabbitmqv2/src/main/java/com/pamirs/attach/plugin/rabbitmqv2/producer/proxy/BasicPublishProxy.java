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

package com.pamirs.attach.plugin.rabbitmqv2.producer.proxy;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import io.shulie.instrument.module.isolation.proxy.impl.ModifyParamShadowMethodProxy;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Method;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/8 15:53
 */
public class BasicPublishProxy extends ModifyParamShadowMethodProxy {

    @Override
    public Object[] fetchParam(Object shadowTarget, Method method, Object... args) {
        String exchange = (String) args[0];
        String routingKey = (String) args[1];
        if (!StringUtils.isBlank(exchange) && !Pradar.isClusterTestPrefix(exchange)) {
            exchange = Pradar.addClusterTestPrefix(exchange);
            args[0] = exchange;
        }

        if (PradarSwitcher.isRabbitmqRoutingkeyEnabled() && !StringUtils.isEmpty(routingKey) && !Pradar.isClusterTestPrefix(routingKey)) {
            routingKey = Pradar.addClusterTestPrefix(routingKey);
            args[1] = routingKey;
        }

        return args;
    }
}
