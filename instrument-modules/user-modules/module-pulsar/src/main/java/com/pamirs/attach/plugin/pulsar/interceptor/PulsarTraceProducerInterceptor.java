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
package com.pamirs.attach.plugin.pulsar.interceptor;

import com.pamirs.attach.plugin.pulsar.common.MQTraceBean;
import com.pamirs.attach.plugin.pulsar.common.MQTraceContext;
import com.pamirs.attach.plugin.pulsar.common.MQType;
import com.pamirs.attach.plugin.pulsar.pub.MQSendMessageTraceLog;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.pulsar.client.impl.MessageImpl;
import org.apache.pulsar.client.impl.ProducerImpl;
import org.apache.pulsar.client.impl.TopicMessageImpl;
import org.apache.pulsar.common.api.proto.PulsarApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/**
 * Create by xuyh at 2020/6/23 15:46.
 */
public class PulsarTraceProducerInterceptor extends AroundInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PulsarTraceProducerInterceptor.class.getName());
    private static ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        try {
            if (!(target instanceof org.apache.pulsar.client.impl.ProducerImpl) && methodName.equals("sendAsync")) {
                return;
            }
            ProducerImpl producer = (ProducerImpl) target;
            if (args == null || args.length != 2) {
                return;
            }
            Object messageObj = args[0];
            MessageImpl<?> message = null;
            if (messageObj instanceof MessageImpl) {
                message = (MessageImpl<?>) messageObj;
            }
            if (messageObj instanceof TopicMessageImpl) {
                TopicMessageImpl<?> topicMessage = (TopicMessageImpl<?>) messageObj;
                message = (MessageImpl<?>) topicMessage.getMessage();
            }
            if (message == null) {
                return;
            }
            MQTraceBean traceBean = new MQTraceBean();
            traceBean.setTopic(Pradar.addClusterTestPrefix(producer.getTopic()));
            traceBean.setKeys(message.getKey());
            traceBean.setBodyLength(message.getData().length);
            traceBean.setClusterTest(String.valueOf(Pradar.isClusterTest()));
            ArrayList<MQTraceBean> traceBeans = new ArrayList<MQTraceBean>(1);
            traceBeans.add(traceBean);
            MQTraceContext mqTraceContext = new MQTraceContext();
            mqTraceContext.setMqType(MQType.PULSAR);
            mqTraceContext.setGroup(producer.getProducerName() + ":" + producer.getTopic());
            mqTraceContext.setTraceBeans(traceBeans);
            MQSendMessageTraceLog.sendMessageBefore(mqTraceContext);

            PulsarApi.MessageMetadata.Builder builder = message.getMessageBuilder();
            for (Map.Entry<String, String> entry : traceBean.getContext().entrySet()) {
                builder.addProperties(org.apache.pulsar.common.api.proto.PulsarApi.KeyValue.newBuilder().setKey(entry.getKey()).setValue(entry.getValue()).build());
            }
            threadLocal.set(mqTraceContext);
        } catch (PradarException e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (PressureMeasureError e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw e;
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }

    @Override
    public void doAfter(Advice advice) {
        MQTraceContext context = (MQTraceContext) threadLocal.get();
        if (context == null) {
            return;
        }
        MQTraceBean traceBean = context.getTraceBeans().get(0);
        context.setSuccess(true);
        MQSendMessageTraceLog.sendMessageAfter(context);
    }

    @Override
    public void doException(Advice advice) {
        if (PradarSwitcher.isTraceEnabled() && PradarSwitcher.isTraceEnabled()) {
            MQTraceContext context = (MQTraceContext) threadLocal.get();
            if (context == null) {
                return;
            }
            MQTraceBean traceBean = context.getTraceBeans().get(0);
            context.setSuccess(false);
            if (PradarSwitcher.isTraceEnabled()) {
                MQSendMessageTraceLog.sendMessageAfter(context);
            }
        }
    }
}
