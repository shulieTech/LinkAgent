package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import java.lang.reflect.Field;

import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;
import com.alibaba.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import com.alibaba.rocketmq.common.message.MessageQueue;

import com.pamirs.attach.plugin.alibaba.rocketmq.common.OrderlyTraceContexts;
import com.pamirs.attach.plugin.alibaba.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/21 2:24 PM
 */
public class OrderlyTraceContextInterceptor extends AroundInterceptor {

    private static Field messageQueueField;

    private static Field defaultMQPushConsumerImplField;

    private static Field this$0Field;

    private final static Logger LOGGER = LoggerFactory.getLogger(OrderlyTraceContextInterceptor.class);

    @Override
    public void doBefore(Advice advice) throws Throwable {
        try {
            MessageQueue messageQueue = getMessageQueue(advice.getTarget());
            String consumeGroup = getConsumeGroup(advice.getTarget());
            ConsumeMessageContext consumeMessageContext = new ConsumeMessageContext();
            consumeMessageContext.setConsumerGroup(consumeGroup);
            consumeMessageContext.setMq(messageQueue);
            consumeMessageContext.setSuccess(false);
            OrderlyTraceContexts.set(consumeMessageContext);
        } catch (Throwable e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            ConsumeMessageContext consumeMessageContext = OrderlyTraceContexts.get();
            if (consumeMessageContext == null || consumeMessageContext.getMsgList() == null) {
                return;
            }
            consumeMessageContext.setSuccess(true);
            consumeMessageContext.setStatus("SUCCESS");
            //兜底，以免after没有执行（consumeMessageContext.getMsgList() != null 说明before 已经执行了）
            PushConsumeMessageHookImpl.getInstance().consumeMessageAfter(consumeMessageContext);
        } catch (Throwable e) {
            LOGGER.error("", e);
        } finally {
            OrderlyTraceContexts.remove();
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        this.doAfter(advice);
    }

    private MessageQueue getMessageQueue(Object target) {
        return ReflectionUtils.getField(getMessageQueueField(target),target);
    }

    private Field getMessageQueueField(Object target) {
        if (messageQueueField == null) {
            messageQueueField = ReflectionUtils.findField(target.getClass(), "messageQueue");
        }
        return messageQueueField;
    }

    private String getConsumeGroup(Object target) {
        Field consumeMessageServiceField = getThis$0Field(target);
        Object consumeMessageService = ReflectionUtils.getField(consumeMessageServiceField, target);;
        Field defaultMQPushConsumerImplField = getDefaultMQPushConsumerImplField(consumeMessageService);
        DefaultMQPushConsumerImpl defaultMQPushConsumer = ReflectionUtils.getField(defaultMQPushConsumerImplField, consumeMessageService);
        return defaultMQPushConsumer.groupName();
    }

    private Field getDefaultMQPushConsumerImplField(Object target) {
        if (defaultMQPushConsumerImplField == null) {
            defaultMQPushConsumerImplField = ReflectionUtils.findField(target.getClass(), "defaultMQPushConsumerImpl");
        }
        return defaultMQPushConsumerImplField;
    }

    private Field getThis$0Field(Object target) {
        if (this$0Field == null) {
            this$0Field = ReflectionUtils.findField(target.getClass(), "this$0");
        }
        return this$0Field;
    }
}
