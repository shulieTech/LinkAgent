package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import java.lang.reflect.Field;
import java.util.List;

import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;
import com.alibaba.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;

import com.pamirs.attach.plugin.alibaba.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/20 5:28 PM
 */
public class ConcurrentlyTraceInterceptor extends AroundInterceptor {

    private static Field messageQueueField;

    private static Field msgsField;

    private static Field defaultMQPushConsumerImplField;

    private static Field this$0Field;

    private final static Logger LOGGER = LoggerFactory.getLogger(ConcurrentlyTraceInterceptor.class);
    private final static ThreadLocal<ConsumeMessageContext> contextThreadLocal = new ThreadLocal<ConsumeMessageContext>();

    private final PushConsumeMessageHookImpl hook = PushConsumeMessageHookImpl.getInstance();

    @Override
    public void doBefore(Advice advice) throws Throwable {
        try {
            MessageQueue messageQueue = getMessageQueue(advice.getTarget());
            List<MessageExt> messageExts = getMessages(advice.getTarget());
            String consumeGroup = getConsumeGroup(advice.getTarget());
            if (messageExts == null || messageExts.isEmpty()) {
                return;
            }
            ConsumeMessageContext consumeMessageContext = new ConsumeMessageContext();
            consumeMessageContext.setConsumerGroup(consumeGroup);
            consumeMessageContext.setMq(messageQueue);
            consumeMessageContext.setMsgList(messageExts);
            consumeMessageContext.setSuccess(false);
            hook.consumeMessageBefore(consumeMessageContext);
            contextThreadLocal.set(consumeMessageContext);
        } catch (Throwable e) {
            LOGGER.error("", e);
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            ConsumeMessageContext consumeMessageContext = contextThreadLocal.get();
            if (consumeMessageContext == null) {
                return;
            }
            consumeMessageContext.setSuccess(true);
            consumeMessageContext.setStatus("CONSUME_SUCCESS");
            hook.consumeMessageAfter(consumeMessageContext);
        } catch (Throwable e) {
            LOGGER.error("", e);
        } finally {
            contextThreadLocal.remove();
        }
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        this.doAfter(advice);
    }

    private MessageQueue getMessageQueue(Object target) {
        return ReflectionUtils.getField(getMessageQueueField(target), target);
    }

    private Field getMessageQueueField(Object target) {
        if (messageQueueField == null) {
            messageQueueField = ReflectionUtils.findField(target.getClass(), "messageQueue");
        }
        return messageQueueField;
    }

    private List<MessageExt> getMessages(Object target) {
        return ReflectionUtils.getField(getMessagesField(target), target);
    }

    private Field getMessagesField(Object target) {
        if (msgsField == null) {
            msgsField = ReflectionUtils.findField(target.getClass(), "msgs");
        }
        return msgsField;
    }

    private String getConsumeGroup(Object target) {
        Field consumeMessageServiceField = getThis$0Field(target);
        Object consumeMessageService = ReflectionUtils.getField(consumeMessageServiceField, target);
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
