package com.pamirs.attach.plugin.apache.rocketmq.interceptor;

import com.pamirs.attach.plugin.apache.rocketmq.common.OrderlyTraceContexts;
import com.pamirs.attach.plugin.apache.rocketmq.hook.PushConsumeMessageHookImpl;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PradarException;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.hook.ConsumeMessageContext;
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
}
