package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

public class ScheduledTaskRegistrarInterceptor extends AroundInterceptor {

    protected final static Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskRegistrarInterceptor.class.getName());

    @Resource
    protected DynamicFieldManager manager;

    @Override
    public void doBefore(Advice advice) {
        Object taskRegistrar = manager.getDynamicField(ScheduledTaskRegistrarInterceptor.class, "taskRegistrar");
        if (taskRegistrar == null) {
            LOGGER.info("cache ScheduledTaskRegistrar for spring shadow task!");
            manager.setDynamicField(ScheduledTaskRegistrarInterceptor.class, "taskRegistrar", advice.getTarget());
        }
    }
}
