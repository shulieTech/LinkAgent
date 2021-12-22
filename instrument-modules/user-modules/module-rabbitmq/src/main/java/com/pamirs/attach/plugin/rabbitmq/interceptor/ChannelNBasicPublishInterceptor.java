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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.attach.plugin.rabbitmq.utils.RabbitMqUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ContextTransfer;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.StringUtils;

/**
 * @Author: guohz
 * @ClassName: ChannelBasicPublishInterceptor
 * @Package: com.pamirs.attach.plugin.rabbitmq.interceptor
 * @Date: 2019-07-25 14:23
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class ChannelNBasicPublishInterceptor extends TraceInterceptorAdaptor {

    public static String X_ORIGINAL_ROUTINGKEY = "x-original-routingKey";

    private volatile Field headersField;

    private synchronized void initHeadersField() {
        try {
            Class clazz = AMQP.BasicProperties.class;
            headersField = clazz.getDeclaredField(RabbitmqConstants.REFLECT_FIELD_HEADERS);
            headersField.setAccessible(true);
        } catch (Throwable e) {
        }
    }

    private void setHeadersField(Object target, Object value) {
        try {
            if (headersField == null) {
                Reflect.on(target).set(RabbitmqConstants.REFLECT_FIELD_HEADERS, value);
            } else {
                headersField.set(target, value);
            }
        } catch (Throwable e) {
            if (e instanceof ReflectException) {
                throw (ReflectException) e;
            }
            Reflect.on(target).set(RabbitmqConstants.REFLECT_FIELD_HEADERS, value);
        }
    }

    @Override
    public void beforeFirst(Advice advice) {
        ClusterTestUtils.validateClusterTest();
        Object[] args = advice.getParameterArray();
        if (Pradar.isClusterTest()) {
            String exchange = (String) args[0];
            String routingKey = (String) args[1];
            if (!StringUtils.isBlank(exchange)) {
                if (!Pradar.isClusterTestPrefix(exchange)) {
                    exchange = Pradar.addClusterTestPrefix(exchange);
                    args[0] = exchange;
                }
            }

            if (PradarSwitcher.isRabbitmqRoutingkeyEnabled()
                    && !StringUtils.isEmpty(routingKey)) {
                if (!Pradar.isClusterTestPrefix(routingKey)) {
                    routingKey = Pradar.addClusterTestPrefix(routingKey);
                    args[1] = routingKey;
                }
            }
            if (StringUtils.isEmpty(exchange) && StringUtils.isEmpty(routingKey)) {
                throw new PressureMeasureError("RabbitMQ发送压测流量exchange和routingKey值传递同时为空或者空字符串，Pradar拒绝推送");
            }
        }
    }

    @Override
    protected ContextTransfer getContextTransfer(Advice advice) {
        Object[] args = advice.getParameterArray();
        AMQP.BasicProperties properties = (AMQP.BasicProperties) args[4];
        final Map<String, Object> headers = properties.getHeaders() == null ? new HashMap<String, Object>()
                : new HashMap<String, Object>(properties.getHeaders());
        headers.putAll(Pradar.getInvokeContextTransformMap());

        if (headers.containsKey(X_ORIGINAL_ROUTINGKEY)) {
            headers.put(X_ORIGINAL_ROUTINGKEY, args[1]);
        }

        if (headersField == null) {
            initHeadersField();
        }

        setHeadersField(properties, Collections.unmodifiableMap(headers));

        return new ContextTransfer() {
            @Override
            public void transfer(String key, String value) {
                headers.put(key, value);
            }
        };
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }

        String exchange = (String) args[0];
        String routingKey = (String) args[1];
        byte[] body = (byte[]) args[5];

        SpanRecord record = new SpanRecord();
        Channel channel = (Channel) advice.getTarget();
        Connection connection = channel.getConnection();
        record.setRemoteIp(connection.getAddress().getHostAddress());
        record.setPort(connection.getPort() + "");
        if (!StringUtil.isEmpty(exchange)) {
            record.setService(exchange);
        } else if (StringUtil.isEmpty(exchange)
                && !StringUtil.isEmpty(routingKey)) {
            record.setService(routingKey);

        }
       /* record.setService(exchange);
        record.setMethod(routingKey);*/
        record.setRequestSize(body.length);
        record.setRequest(body);

        return record;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (args == null || args.length == 0) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

    @Override
    public String getPluginName() {
        return RabbitmqConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return RabbitmqConstants.PLUGIN_TYPE;
    }
}
