package com.pamirs.attach.plugin.alibaba.rocketmq.interceptor;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import com.alibaba.rocketmq.client.hook.ConsumeMessageContext;
import com.pamirs.attach.plugin.alibaba.rocketmq.common.OrderlyTraceContexts;
import com.pamirs.attach.plugin.alibaba.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/12/21 3:08 PM
 */
public class OrderlyTraceAfterInterceptor extends AroundInterceptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(OrderlyTraceContextInterceptor.class);

    private final PushConsumeMessageHookImpl hook = PushConsumeMessageHookImpl.getInstance();

    @Override
    public void doAfter(Advice advice) throws Throwable {
        try {
            ConsumeOrderlyStatus status = (ConsumeOrderlyStatus) advice.getParameterArray()[1];
            ConsumeMessageContext consumeMessageContext = OrderlyTraceContexts.get();
            if (consumeMessageContext == null || consumeMessageContext.getMsgList() == null) {
                return;
            }
            consumeMessageContext.setSuccess(ConsumeOrderlyStatus.SUCCESS == status
                    || ConsumeOrderlyStatus.COMMIT == status);
            consumeMessageContext.setStatus(status.name());
            hook.consumeMessageAfter(consumeMessageContext);
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
}
