package com.pamirs.attach.plugin.jms.interceptor;

import com.pamirs.attach.plugin.jms.util.ApplyFunction;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Queue;
import javax.naming.Context;

public class LookupInterceptor extends ParametersWrapperInterceptorAdaptor {
    public static ThreadLocal<ApplyFunction<Queue>> applyFunctionOfGetQueueThreadLocal = new ThreadLocal<ApplyFunction<Queue>>();
    public static ThreadLocal<String> JNDI_NAME = new ThreadLocal<String>();
    protected final static Logger LOGGER = LoggerFactory.getLogger(LookupInterceptor.class.getName());
    @Override
    protected Object[] getParameter0(final Advice advice) throws Throwable {
        final Object[] args = advice.getParameterArray();
        ClusterTestUtils.validateClusterTest();
        if (args == null || args.length == 0) {
            return args;
        }
        String jndiName = (String)args[0];
        JNDI_NAME.set(jndiName);
        final Context context = (Context)advice.getTarget();
        final String ptJndiName = Pradar.addClusterTestPrefix(jndiName);
        final ApplyFunction<Queue> applyFunction = new ApplyFunction<Queue>() {
            @Override
            public Queue apply(Object... args) throws Throwable {
                JNDI_NAME.set(ptJndiName);
                return (Queue)context.lookup(ptJndiName);
            }
        };
        applyFunctionOfGetQueueThreadLocal.set(applyFunction);
        return super.getParameter0(advice);
    }
}
