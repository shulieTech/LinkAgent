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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/08/31 10:21 下午
 */
public class StrictExceptionHandlerInterceptor extends AroundInterceptor {

    private static Logger logger = LoggerFactory.getLogger(ChannelNProcessDeliveryInterceptor.class.getName());

    @Override
    public void doBefore(Advice advice) throws Throwable {
        if (advice.getParameterArray().length >= 2 && advice.getParameterArray()[1] instanceof Throwable) {
            logger.warn("[RabbitMQ] has error!", (Throwable)advice.getParameterArray()[1]);
        }
    }

}
