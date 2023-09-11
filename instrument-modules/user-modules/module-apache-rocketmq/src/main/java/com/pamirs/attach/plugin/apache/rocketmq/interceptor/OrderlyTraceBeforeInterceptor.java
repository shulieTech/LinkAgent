package com.pamirs.attach.plugin.apache.rocketmq.interceptor;

import com.pamirs.attach.plugin.apache.rocketmq.common.OrderlyTraceContexts;
import com.pamirs.attach.plugin.apache.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/21 2:28 PM
 */
public class OrderlyTraceBeforeInterceptor extends AroundInterceptor {

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            ConsumeMessageContext consumeMessageContext = OrderlyTraceContexts.get();
            if (consumeMessageContext == null) {
                return;
            }
            if (consumeMessageContext.getMsgList() != null && !consumeMessageContext.getMsgList().isEmpty()) {
                return;
            }
            List<MessageExt> messageExts;
            if ("consumeMessage".equals(advice.getBehaviorName())) {
                messageExts = (List<MessageExt>) advice.getParameterArray()[0];
            } else {
                messageExts = (List<MessageExt>) advice.getReturnObj();
            }
            if (messageExts == null || messageExts.isEmpty()) {
                return;
            }
            consumeMessageContext.setMsgList(messageExts);
            PushConsumeMessageHookImpl.getInstance().consumeMessageBefore(consumeMessageContext);
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
