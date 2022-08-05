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

package com.pamirs.attach.plugin.apache.rocketmqv2.producer.proxy;


import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import io.shulie.instrument.module.isolation.proxy.impl.ModifyParamShadowMethodProxy;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/4 16:07
 */
public class SendProxy extends ModifyParamShadowMethodProxy {

    @Override
    public Object[] fetchParam(Object shadowTarget, Method method, Object... args) {
        Message msg = (Message) args[0];
        String topic = msg.getTopic();
        if (topic != null && !Pradar.isClusterTestPrefix(topic)) {
            msg.setTopic(Pradar.addClusterTestPrefix(topic));
        }
        msg.putUserProperty(PradarService.PRADAR_CLUSTER_TEST_KEY, Boolean.TRUE.toString());

        for (int i = 0, len = args.length; i < len; i++) {
            if (!(args[i] instanceof SendCallback) || args[i] instanceof PradarSendCallback) {
                continue;
            }
            final SendCallback sendCallback = (SendCallback) args[i];
            final Map<String, String> context = PradarService.getInvokeContext();
            args[i] = new PradarSendCallback(sendCallback, context);
        }

        return args;
    }

    static class PradarSendCallback implements SendCallback {

        private final SendCallback sendCallback;

        private final Map<String, String> context;

        public PradarSendCallback(SendCallback sendCallback, Map<String, String> context) {
            this.sendCallback = sendCallback;
            this.context = context;
        }

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
    }
}
