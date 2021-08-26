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

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import java.util.Map;

/**
 * @author angju
 * @date 2020/8/23 16:36
 */
@Destroyable(RabbitmqDestroy.class)
public class SpringRabbitRabbitAdminDeclareQueueInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return RabbitmqConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RabbitmqConstants.PLUGIN_TYPE;
    }

    @Override
    public void beforeFirst(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        Queue targetQueue = (Queue) args[0];
        Map<String, Object> arguments = targetQueue.getArguments();
        if (ClusterTestUtils.isClusterTestRequest((String) arguments.get(PradarService.PRADAR_CLUSTER_TEST_KEY))) {
            return;
        }
        String ptQueueName = Pradar.addClusterTestPrefix(targetQueue.getName());
        arguments.put(PradarService.PRADAR_CLUSTER_TEST_KEY, Boolean.TRUE.toString());
        Queue ptQueue = new Queue(ptQueueName, targetQueue.isDurable(), targetQueue.isExclusive(), targetQueue.isAutoDelete(), targetQueue.getArguments());

        RabbitAdmin rabbitAdmin = (RabbitAdmin) target;
        rabbitAdmin.declareQueue(ptQueue);
    }
}
