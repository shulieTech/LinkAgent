package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import java.lang.reflect.Field;

import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;
import com.alibaba.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import com.alibaba.rocketmq.common.message.MessageQueue;

import com.pamirs.attach.plugin.alibaba.rocketmq.common.OrderlyTraceContexts;
import com.pamirs.attach.plugin.alibaba.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
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
        } finally {
            OrderlyTraceContexts.remove();
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        this.doAfter(advice);
    }

    private MessageQueue getMessageQueue(Object target) {
        return Reflect.on(target).get(getMessageQueueField(target));
    }

    private Field getMessageQueueField(Object target) {
        if (messageQueueField == null) {
            messageQueueField = Reflect.on(target).field0("messageQueue");
        }
        return messageQueueField;
    }

    private String getConsumeGroup(Object target) {
        Field consumeMessageServiceField = getThis$0Field(target);
        Object consumeMessageService = Reflect.on(target).get(consumeMessageServiceField);
        Field defaultMQPushConsumerImplField = getDefaultMQPushConsumerImplField(consumeMessageService);
        DefaultMQPushConsumerImpl defaultMQPushConsumer = Reflect.on(consumeMessageService).get(
            defaultMQPushConsumerImplField);
        return defaultMQPushConsumer.groupName();
    }

    private Field getDefaultMQPushConsumerImplField(Object target) {
        if (defaultMQPushConsumerImplField == null) {
            defaultMQPushConsumerImplField = Reflect.on(target).field0("defaultMQPushConsumerImpl");
        }
        return defaultMQPushConsumerImplField;
    }

    private Field getThis$0Field(Object target) {
        if (this$0Field == null) {
            this$0Field = Reflect.on(target).field0("this$0");
        }
        return this$0Field;
    }
}
