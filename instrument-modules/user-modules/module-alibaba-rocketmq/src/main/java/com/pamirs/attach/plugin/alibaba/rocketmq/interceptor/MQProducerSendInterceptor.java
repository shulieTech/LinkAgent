/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import com.alibaba.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.pamirs.attach.plugin.alibaba.rocketmq.hook.SendMessageHookImpl;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MQProducerSendInterceptor extends AroundInterceptor {


    private Object lock = new Object();


    private static Set registerHookSet = new HashSet();

    @Override
    public void doBefore(Advice advice) {
        if (advice.getTarget() instanceof DefaultMQProducerImpl && !((DefaultMQProducerImpl) advice.getTarget()).hasSendMessageHook()) {
            synchronized (lock) {
                if (!((DefaultMQProducerImpl) advice.getTarget()).hasSendMessageHook()) {
                    ((DefaultMQProducerImpl) advice.getTarget()).registerSendMessageHook(new SendMessageHookImpl());
                    LOGGER.warn("MQProducerSendInterceptor 注册发送trace hook成功");
                }
            }
        }

        Object[] args = advice.getParameterArray();
        Message msg = (Message) args[0];
        if (PradarSwitcher.isClusterTestEnabled()) {
            if (Pradar.isClusterTest()) {
                String topic = msg.getTopic();
                String testTopic = Pradar.addClusterTestPrefix(topic);
                if (topic != null
                        && !Pradar.isClusterTestPrefix(topic)) {
                    msg.setTopic(testTopic);
                }
                msg.putUserProperty(PradarService.PRADAR_CLUSTER_TEST_KEY, Boolean.TRUE.toString());
                // 设置messageQueue
                if (args.length > 1 && args[1] instanceof MessageQueue) {
                    MessageQueue queue = (MessageQueue) args[1];
                    if (!Pradar.isClusterTestPrefix(queue.getTopic())) {
                        queue.setTopic(testTopic);
                    }
                }
            }
        }


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
}
