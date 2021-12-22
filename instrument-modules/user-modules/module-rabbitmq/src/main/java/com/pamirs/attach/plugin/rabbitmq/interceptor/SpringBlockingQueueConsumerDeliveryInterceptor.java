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

import com.pamirs.attach.plugin.rabbitmq.RabbitmqConstants;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.attach.plugin.rabbitmq.utils.RabbitMqUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.springframework.amqp.core.Message;

import java.util.HashMap;
import java.util.Map;


/**
 * @Author: guohz
 * @ClassName: ChannelBasicGetInterceptor
 * @Package: com.pamirs.attach.plugin.rabbitmq.interceptor
 * @Date: 2019-07-25 14:33
 * @Description:
 */
@Destroyable(RabbitmqDestroy.class)
public class SpringBlockingQueueConsumerDeliveryInterceptor extends TraceInterceptorAdaptor {

    @Override
    protected boolean isClient(Advice advice) {
        return false;
    }


    @Override
    public void beforeLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return;
        }
        Message message = (Message) args[1];
        if (message == null) {
            return;
        }

        validatePressureMeasurement(message);
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return null;
        }
        Message message = (Message) args[1];
        if (message == null) {
            return null;
        }
        Channel channel = (Channel) args[0];
        Connection connection = channel.getConnection();
        SpanRecord record = new SpanRecord();
        record.setRemoteIp(connection.getAddress().getHostAddress());
        record.setPort(connection.getPort() + "");

        String queue = message.getMessageProperties().getConsumerQueue();
        String exchange = message.getMessageProperties().getReceivedExchange();
        if (!StringUtil.isEmpty(queue)) {
            record.setService(queue);
        } else if (StringUtil.isEmpty(queue)
                && !StringUtil.isEmpty(exchange)) {
            record.setService(exchange);
        }

     /*   record.setService(message.getMessageProperties().getConsumerQueue());
        record.setMethod(message.getMessageProperties().getReceivedExchange());*/
        byte[] body = message.getBody();
        record.setRequestSize(body.length);
        record.setRequest(body);
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        if (headers != null) {
            Map<String, String> rpcContext = new HashMap<String, String>();
            for (String key : Pradar.getInvokeContextTransformKeys()) {
                String value = ObjectUtils.toString(headers.get(key));
                if (value != null) {
                    rpcContext.put(key, value);
                }
            }
            record.setContext(rpcContext);
        }
        return record;
    }


    @Override
    public SpanRecord afterTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return null;
        }
        Message message = (Message) args[1];
        if (message == null) {
            return null;
        }

        SpanRecord record = new SpanRecord();
        return record;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        if (ArrayUtils.isEmpty(args) || args.length != 2 || args[1] == null) {
            return null;
        }
        Message message = (Message) args[1];
        if (message == null) {
            return null;
        }
        SpanRecord record = new SpanRecord();
        record.setResultCode(ResultCode.INVOKE_RESULT_FAILED);
        record.setResponse(advice.getThrowable());
        return record;
    }

    private void validatePressureMeasurement(Message message) {
        try {
            Pradar.setClusterTest(false);
            String queue = message.getMessageProperties().getConsumerQueue();
            String exchange = message.getMessageProperties().getReceivedExchange();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            if (Pradar.isClusterTestPrefix(queue)) {
                Pradar.setClusterTest(true);
            } else if (Pradar.isClusterTestPrefix(exchange)) {
                Pradar.setClusterTest(true);
            } else if (Pradar.isClusterTestPrefix(routingKey)) {
                Pradar.setClusterTest(true);
            }
        } catch (Throwable e) {
            LOGGER.error("", e);
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(e);
            }
        }
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
