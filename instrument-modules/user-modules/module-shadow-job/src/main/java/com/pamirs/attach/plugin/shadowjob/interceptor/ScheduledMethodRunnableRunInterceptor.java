/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.attach.plugin.shadowjob.ShadowJobConstants;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * @author angju
 * @date 2021/3/25 20:43
 */
public class ScheduledMethodRunnableRunInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) advice.getTarget();
        String className = scheduledMethodRunnable.getTarget().getClass().getName();
        String methodName = scheduledMethodRunnable.getMethod().getName();
        String key = className + "." + methodName;
        if (GlobalConfig.getInstance().getNeedRegisterJobs() != null &&
                GlobalConfig.getInstance().getNeedRegisterJobs().containsKey(key) &&
                GlobalConfig.getInstance().getRegisteredJobs() != null &&
                !GlobalConfig.getInstance().getRegisteredJobs().containsKey(key)) {
            boolean result = registerShadowJob(key, GlobalConfig.getInstance().getNeedRegisterJobs().get(key));
            if (result) {
                GlobalConfig.getInstance().getNeedRegisterJobs().get(key).setActive(0);
                GlobalConfig.getInstance().addRegisteredJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(key));
                GlobalConfig.getInstance().getNeedRegisterJobs().remove(key);
            }
        }

        if (GlobalConfig.getInstance().getNeedStopJobs() != null &&
                GlobalConfig.getInstance().getNeedStopJobs().containsKey(key) &&
                GlobalConfig.getInstance().getRegisteredJobs().containsKey(key)) {
            boolean result = disableShaDowJob(GlobalConfig.getInstance().getNeedStopJobs().get(key));
            if (result) {
                GlobalConfig.getInstance().getNeedStopJobs().remove(key);
                GlobalConfig.getInstance().getRegisteredJobs().remove(key);
            }
        }
        return advice.getParameterArray();
    }


    private static ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private Map<String, ScheduledFuture> schedulerFuture = new HashMap<String, ScheduledFuture>();

    static {
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
    }

    public static void release() {
        threadPoolTaskScheduler.shutdown();
        threadPoolTaskScheduler = null;

    }

    @Override
    protected void clean() {
        for (Map.Entry<String, ScheduledFuture> entry : schedulerFuture.entrySet()) {
            ScheduledFuture future = entry.getValue();
            if (future != null && !future.isCancelled() && !future.isDone()) {
                future.cancel(true);
            }
        }
        schedulerFuture.clear();
    }

    public boolean registerShadowJob(final String key, final ShadowJob shaDowJob) throws Throwable {
        if (PradarSpringUtil.getBeanFactory() == null || schedulerFuture.containsKey(shaDowJob.getClassName())) {
            LOGGER.warn("PradarSpringUtil.getBeanFactory is null,can not register shaDowJob " + shaDowJob.getClassName());
            return false;
        }
        //已经注册过
        if (schedulerFuture.containsKey(key)) {
            return true;
        }
        String cron = shaDowJob.getCron();
        final String className = shaDowJob.getClassName();
        // 通过springboot applicationContext找到注册的job
        ScheduledAnnotationBeanPostProcessor bean = PradarSpringUtil.getBeanFactory().getBean(ScheduledAnnotationBeanPostProcessor.class);
        Field registrar = ScheduledAnnotationBeanPostProcessor.class.getDeclaredField("registrar");
        registrar.setAccessible(true);
        ScheduledTaskRegistrar scheduledTaskRegistrar = (ScheduledTaskRegistrar) registrar.get(bean);
        List<CronTask> cronTasks = Reflect.on(scheduledTaskRegistrar).get(ShadowJobConstants.DYNAMIC_FIELD_SPRING_CRON_TASKS);
        // 从业务应用注册的schedule任务找到需要注册的影子job
        boolean registeRes = false;
        for (final CronTask cronTask : cronTasks) {
            ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) cronTask.getRunnable();
            final String c = scheduledMethodRunnable.getTarget().getClass().getName();
            final String m = scheduledMethodRunnable.getMethod().getName();
            if (key.equals(cronTask.toString()) || key.equals(c + "." + m)) {
                // 注册影子job
                Trigger trigger = new CronTrigger(cron);
                ScheduledFuture<?> schedule = threadPoolTaskScheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        // 为了打压测标识
                        Pradar.startTrace(null, c, m);
                        Pradar.setClusterTest(true);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("shadow-job run, className:" + className);
                        }
                        cronTask.getRunnable().run();
                        Pradar.setClusterTest(false);
                        Pradar.endTrace();
                    }
                }, trigger);
                schedulerFuture.put(shaDowJob.getClassName(), schedule);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("shadow-job registe success:" + className);
                }
                registeRes = true;
                break;
            }
        }
        if (!registeRes) {
            shaDowJob.setErrorMessage("未找到应用已注册真实job:" + className);
        }
        return registeRes;
    }

    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        ScheduledFuture scheduledFuture = schedulerFuture.get(shaDowJob.getClassName());
        if (null != scheduledFuture) {
            scheduledFuture.cancel(true);
            return true;
        } else {
            shaDowJob.setErrorMessage(shaDowJob.getClassName() + "未注册or已停止！");
            return false;
        }
    }
}
