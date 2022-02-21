package com.pamirs.attach.plugin.apache.rocketmq.interceptor;

import java.lang.reflect.Field;
import java.util.List;

import com.pamirs.attach.plugin.apache.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
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
            ConsumeMessageContext consumeMessageContext = contextThreadLocal.get();
            if (consumeMessageContext == null) {
                return;
            }
            consumeMessageContext.setSuccess(true);
            consumeMessageContext.setStatus("CONSUME_SUCCESS");
            hook.consumeMessageAfter(consumeMessageContext);
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
            contextThreadLocal.remove();
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

    private List<MessageExt> getMessages(Object target) {
        return Reflect.on(target).get(getMessagesField(target));
    }

    private Field getMessagesField(Object target) {
        if (msgsField == null) {
            msgsField = Reflect.on(target).field0("msgs");
        }
        return msgsField;
    }

    private String getConsumeGroup(Object target) {
        Field consumeMessageServiceField = getThis$0Field(target);
        Object consumeMessageService = Reflect.on(target).get(consumeMessageServiceField);
        Field defaultMQPushConsumerImplField = getDefaultMQPushConsumerImplField(consumeMessageService);
        DefaultMQPushConsumerImpl defaultMQPushConsumer = Reflect.on(consumeMessageService).get(defaultMQPushConsumerImplField);
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
