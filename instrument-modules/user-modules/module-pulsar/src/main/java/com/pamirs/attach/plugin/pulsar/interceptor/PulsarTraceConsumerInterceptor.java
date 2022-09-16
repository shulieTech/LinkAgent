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
package com.pamirs.attach.plugin.pulsar.interceptor;

import com.pamirs.attach.plugin.pulsar.common.MQTraceBean;
import com.pamirs.attach.plugin.pulsar.common.MQTraceContext;
import com.pamirs.attach.plugin.pulsar.common.MQType;
import com.pamirs.attach.plugin.pulsar.sub.MQConsumeMessageTraceLog;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.ObjectUtils;
import org.apache.pulsar.client.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Create by xuyh at 2020/6/23 15:46.
 */
public class PulsarTraceConsumerInterceptor extends AroundInterceptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PulsarTraceConsumerInterceptor.class.getName());
    private static ThreadLocal<Object> threadLocal = new ThreadLocal<Object>();

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        try {
            if (!(target instanceof org.apache.pulsar.client.impl.ConsumerBase)) {
                return;
            }
            if (args == null || args.length != 1) {
                return;
            }

            Object messageObj = args[0];
            if (!(messageObj instanceof Message)) {
                return;
            }
            Message<?> message = (Message<?>) messageObj;

            String topic = message.getTopicName();
            //persistent://public/default/demo-topic
            if (topic.startsWith("persistent:")) {
                topic = topic.substring(topic.lastIndexOf("/") + 1);
            }

            MQTraceContext mqTraceContext = new MQTraceContext();
            mqTraceContext.setMqType(MQType.PULSAR);
            mqTraceContext.setGroup(message.getProducerName() + ":" + topic);
            MQTraceBean traceBean = new MQTraceBean();
            Map<String, String> properties = message.getProperties();
            if (properties != null) {
                Map<String, String> rpcContext = new HashMap<String, String>();
                for (String key : Pradar.getInvokeContextTransformKeys()) {
                    String value = ObjectUtils.toString(properties.get(key));
                    if (value != null) {
                        rpcContext.put(key, value);
                    }
                }
                traceBean.setContext(rpcContext);
            }
            traceBean.setTopic(topic);
            traceBean.setKeys(message.getKey());
            traceBean.setBodyLength(message.getData().length);
            // topic是否PT_开头
            boolean isClusterTest = Pradar.isClusterTestPrefix(topic);
            // 消息的properties是否包含Pradar.PRADAR_CLUSTER_TEST_KEY
            if (properties != null) {
                String clusterTestValue = ObjectUtils.toString(properties.get(PradarService.PRADAR_CLUSTER_TEST_KEY));
                isClusterTest = isClusterTest || Boolean.TRUE.toString().equals(clusterTestValue) || "1".equals(clusterTestValue);
            }
            if (isClusterTest) {
                traceBean.setClusterTest(Boolean.TRUE.toString());
            }
            ArrayList<MQTraceBean> traceBeans = new ArrayList<MQTraceBean>(1);
            traceBeans.add(traceBean);
            mqTraceContext.setTraceBeans(traceBeans);
            MQConsumeMessageTraceLog.consumeMessageBefore(mqTraceContext);
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
        recordAfter(true);
    }


    @Override
    public void doException(Advice advice) {
        recordAfter(false);
    }

    private void recordAfter(boolean flag) {
        try {
            MQTraceContext context = (MQTraceContext) threadLocal.get();
            if (context == null) {
                return;
            }
            context.setSuccess(flag);
            MQConsumeMessageTraceLog.consumeMessageAfter(context);
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
}
