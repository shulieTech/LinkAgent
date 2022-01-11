package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import java.util.concurrent.Callable;

public class QueueConnectionFactoryInterceptor extends ParametersWrapperInterceptorAdaptor {

    protected static ThreadLocal<Callable<QueueConnection>> callableOfGetQueueConnectionThreadLocal = new ThreadLocal<Callable<QueueConnection>>();

    @Override
    protected Object[] getParameter0(final Advice advice) throws Throwable {
        final QueueConnectionFactory queueConnectionFactory = (QueueConnectionFactory)advice.getTarget();
        final Callable<QueueConnection> callableOfGetQueueConnection = new Callable<QueueConnection>() {
            @Override
            public QueueConnection call() throws Exception {
                return queueConnectionFactory.createQueueConnection();
            }
        };
        callableOfGetQueueConnectionThreadLocal.set(callableOfGetQueueConnection);
        return advice.getParameterArray();
    }
}
