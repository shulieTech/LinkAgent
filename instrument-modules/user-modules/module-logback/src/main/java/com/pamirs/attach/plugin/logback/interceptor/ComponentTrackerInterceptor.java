package com.pamirs.attach.plugin.logback.interceptor;

import com.pamirs.attach.plugin.logback.utils.AppenderHolder;
import com.pamirs.attach.plugin.logback.utils.ClusterTestMarker;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/22 10:21 上午
 */
public class ComponentTrackerInterceptor extends ResultInterceptorAdaptor {

    protected boolean isBusinessLogOpen;
    protected String bizShadowLogPath;

    private final Logger log = LoggerFactory.getLogger(ComponentTrackerInterceptor.class);

    public ComponentTrackerInterceptor(boolean isBusinessLogOpen, String bizShadowLogPath) {
        this.isBusinessLogOpen = isBusinessLogOpen;
        this.bizShadowLogPath = bizShadowLogPath;
    }

    @Override
    public Object getResult0(Advice advice) {
        if (!this.isBusinessLogOpen) {
            return advice.getReturnObj();
        }
        Object appender = advice.getReturnObj();
        try {
            Object ptAppender = AppenderHolder.getOrCreatePtAppender(appender.getClass().getClassLoader(),
                appender, bizShadowLogPath);
            if (ClusterTestMarker.isClusterTestThenClear()) {
                return ptAppender == null ? appender : ptAppender;
            }
        } catch (ClassNotFoundException e) {
            log.error("get pt appender fail!", e);
        }
        return appender;
    }
}
