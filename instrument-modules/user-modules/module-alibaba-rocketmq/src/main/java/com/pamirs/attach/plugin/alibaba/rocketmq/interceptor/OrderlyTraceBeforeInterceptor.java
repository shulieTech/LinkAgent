package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import java.util.List;

import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;
import com.alibaba.rocketmq.common.message.MessageExt;

import com.pamirs.attach.plugin.alibaba.rocketmq.common.OrderlyTraceContexts;
import com.pamirs.attach.plugin.alibaba.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

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
            List<MessageExt> messageExts = (List<MessageExt>)advice.getReturnObj();
            if (messageExts == null || messageExts.isEmpty()) {
                return;
            }
            consumeMessageContext.setMsgList(messageExts);
            PushConsumeMessageHookImpl.getInstance().consumeMessageBefore(consumeMessageContext);
        } catch (Throwable e) {
            LOGGER.error("", e);
        }
    }
}
