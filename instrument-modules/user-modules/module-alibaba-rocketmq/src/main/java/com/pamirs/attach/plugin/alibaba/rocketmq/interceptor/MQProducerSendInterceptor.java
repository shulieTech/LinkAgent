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
package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import com.alibaba.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

import com.pamirs.attach.plugin.alibaba.rocketmq.hook.SendMessageHookImpl;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MQProducerSendInterceptor extends AroundInterceptor {

    private static Set registerHookSet = new HashSet();

    @Override
    public void doBefore(Advice advice) {
        DefaultMQProducerImpl defaultMQProducerImpl = null;
        if (advice.getTarget() instanceof DefaultMQProducer) {
            defaultMQProducerImpl = ((DefaultMQProducer)advice.getTarget()).getDefaultMQProducerImpl();
        }
        if (advice.getTarget() instanceof DefaultMQProducerImpl) {
            defaultMQProducerImpl = (DefaultMQProducerImpl)advice.getTarget();
        }
        if (defaultMQProducerImpl != null) {
            if (!registerHookSet.contains(defaultMQProducerImpl)) {
                synchronized (this) {
                    if (!registerHookSet.contains(defaultMQProducerImpl)) {
                        ((DefaultMQProducerImpl)defaultMQProducerImpl).registerSendMessageHook(new SendMessageHookImpl());
                        registerHookSet.add(defaultMQProducerImpl);
                        LOGGER.warn("MQProducerSendInterceptor 注册发送trace hook成功");
                    }
                }
            }
        }

        Message msg = (Message) advice.getParameterArray()[0];
        ClusterTestUtils.validateClusterTest();
        if (Pradar.isClusterTest()) {
            String topic = msg.getTopic();
            if (topic != null && !Pradar.isClusterTestPrefix(topic)) {
                msg.setTopic(Pradar.addClusterTestPrefix(topic));
            }
            msg.putUserProperty(PradarService.PRADAR_CLUSTER_TEST_KEY, Boolean.TRUE.toString());
        }

        Object[] args = advice.getParameterArray();
        for (int i = 0, len = args.length; i < len; i++) {
            if (!(args[i] instanceof SendCallback)) {
                continue;
            }
            final SendCallback sendCallback = (SendCallback) args[i];
            final Map<String, String> context = PradarService.getInvokeContext();
            advice.changeParameter(i, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    try {
                        Pradar.setInvokeContext(context);
                        sendCallback.onSuccess(sendResult);
                    } finally {
                        Pradar.clearInvokeContext();
                    }
                }

                @Override
                public void onException(Throwable e) {
                    try {
                        Pradar.setInvokeContext(context);
                        sendCallback.onException(e);
                    } finally {
                        Pradar.clearInvokeContext();
                    }
                }
            });
        }
    }

    @Override
    public void doAfter(Advice advice) {
    }

    @Override
    public void doException(Advice advice) {
    }
}
