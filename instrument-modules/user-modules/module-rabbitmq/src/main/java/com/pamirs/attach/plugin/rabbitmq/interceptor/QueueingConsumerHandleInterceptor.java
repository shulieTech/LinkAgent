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

import java.util.HashMap;
import java.util.Map;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarService;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ReversedTraceInterceptorAdaptor;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @Author: guohz
 * @ClassName: QueueConsumerDeliveryInterceptor
 * @Package: com.pamirs.attach.plugin.rabbitmq.interceptor
 * @Date: 2019-07-25 14:33
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class QueueingConsumerHandleInterceptor extends ReversedTraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return RabbitmqConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RabbitmqConstants.PLUGIN_TYPE;
    }

    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }

    @Override
    public void beforeLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args[0] == null) {
            return;
        }
        Delivery queue = (Delivery)args[0];
        Envelope envelope = queue.getEnvelope();
        if (envelope == null) {
            return;
        }

        final AMQP.BasicProperties properties = queue.getProperties();
        validatePressureMeasurement(envelope, properties);
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }

        Object target = advice.getTarget();
        Class<?>[] classes = target.getClass().getClasses();
        if (classes.length != 1) {
            return null;
        }
        Object deliveryObj = args[0];
        if (deliveryObj == null) {
            return null;
        }

        SpanRecord record = new SpanRecord();
        try {
            Envelope envelope = Reflect.on(deliveryObj).get(RabbitmqConstants.DYNAMIC_FIELD_ENVELOPE);
            if (envelope == null) {//说明是毒药，已经报错了
                return null;
            }
            record.setService(envelope.getExchange());
            record.setMethod(envelope.getRoutingKey());
        } catch (ReflectException e) {
        }

        try {
            AMQP.BasicProperties properties = Reflect.on(deliveryObj).get(RabbitmqConstants.DYNAMIC_FIELD_PROPERTIES);
            Map<String, Object> headers = properties.getHeaders();
            if (headers != null) {
                Map<String, String> rpcContext = new HashMap<String, String>();
                for (String key : Pradar.getInvokeContextTransformKeys()) {
                    Object tmp = headers.get(key);
                    if (tmp != null) {
                        String value = tmp.toString();
                        if (!StringUtil.isEmpty(value)) {
                            rpcContext.put(key, value);
                        }
                    }
                }
                record.setContext(rpcContext);
            }
        } catch (ReflectException e) {
        }
        try {
            byte[] body = Reflect.on(deliveryObj).get(RabbitmqConstants.DYNAMIC_FIELD_BODY);
            record.setRequestSize(body.length);
            record.setRequest(body);
        } catch (ReflectException e) {
        }

        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }

        Object target = advice.getTarget();
        Class<?>[] classes = target.getClass().getClasses();
        if (classes.length != 1) {
            return null;
        }
        Object deliveryObj = args[0];
        if (deliveryObj == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }

        Object target = advice.getTarget();
        Class<?>[] classes = target.getClass().getClasses();
        if (classes.length != 1) {
            return null;
        }
        Object deliveryObj = args[0];
        if (deliveryObj == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

    private void validatePressureMeasurement(Envelope envelope, AMQP.BasicProperties properties) {
        try {
            Pradar.setClusterTest(false);
            if (envelope == null) {
                return;
            }

            String exchange = envelope.getExchange();
            exchange = StringUtils.trimToEmpty(exchange);
            String routingKey = envelope.getRoutingKey();
            if (exchange != null
                && Pradar.isClusterTestPrefix(exchange)) {
                Pradar.setClusterTest(true);
            } else if (PradarSwitcher.isRabbitmqRoutingkeyEnabled()
                && routingKey != null
                && Pradar.isClusterTestPrefix(routingKey)) {
                Pradar.setClusterTest(true);
            } else if (null != properties.getHeaders() && ClusterTestUtils.isClusterTestRequest(
                ObjectUtils.toString(properties.getHeaders().get(PradarService.PRADAR_CLUSTER_TEST_KEY)))) {
                Pradar.setClusterTest(true);
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
    }
}
