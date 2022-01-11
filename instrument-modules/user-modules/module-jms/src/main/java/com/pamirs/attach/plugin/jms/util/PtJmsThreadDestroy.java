package com.pamirs.attach.plugin.jms.util;

import com.pamirs.attach.plugin.jms.interceptor.JmsReceiveInterceptor;
import com.pamirs.attach.plugin.jms.interceptor.LookupInterceptor;
import com.shulie.instrument.simulator.api.listener.Destroyed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PtJmsThreadDestroy implements Destroyed {
    protected final static Logger LOGGER = LoggerFactory.getLogger(PtJmsThreadDestroy.class.getName());
    @Override
    public void destroy() {
        LookupInterceptor.JNDI_NAME.remove();
        LookupInterceptor.applyFunctionOfGetQueueThreadLocal.remove();

        for (List<ChinaLifeWorker> value : JmsReceiveInterceptor.RUNNING_JNDI_THREAD_MAP.values()) {
            for (ChinaLifeWorker chinaLifeWorker : value) {
                try {
                    chinaLifeWorker.setStop(true);
                    chinaLifeWorker.interrupt();
                } catch (Throwable e){
                    LOGGER.error("PtJmsThreadDestroy error", e);
                }
            }
        }
    }
}
