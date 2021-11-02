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
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Date;
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

    public boolean registerShadowJob(final String key, final ShadowJob shadowJob) throws Throwable {
        if (PradarSpringUtil.getBeanFactory() == null || schedulerFuture.containsKey(shadowJob.getClassName())) {
            LOGGER.warn("PradarSpringUtil.getBeanFactory is null,can not register shadowJob " + shadowJob.getClassName());
            return false;
        }
        //已经注册过
        if (schedulerFuture.containsKey(key)) {
            return true;
        }
        // 通过springboot applicationContext找到注册的job
        ScheduledAnnotationBeanPostProcessor bean = PradarSpringUtil.getBeanFactory().getBean(ScheduledAnnotationBeanPostProcessor.class);
        Field registrar = ScheduledAnnotationBeanPostProcessor.class.getDeclaredField("registrar");
        registrar.setAccessible(true);
        ScheduledTaskRegistrar scheduledTaskRegistrar = (ScheduledTaskRegistrar) registrar.get(bean);

        // 注册影子任务
        boolean registeredRes = registerTask(scheduledTaskRegistrar, shadowJob, key);

        if (!registeredRes) {
            shadowJob.setErrorMessage("未找到应用已注册真实job:" + shadowJob.getClassName());
        }
        return registeredRes;
    }

    private boolean registerTask(ScheduledTaskRegistrar scheduledTaskRegistrar, final ShadowJob shadowJob, String key) {
        boolean hasIntervalConfig = StringUtils.hasText(shadowJob.getCron()) || shadowJob.getFixedRate() != null || shadowJob.getFixedDelay() != null;
        if (!hasIntervalConfig) {
            LOGGER.error("影子任务必须参数不存在, cron, fixedRate, fixedDelay 三者需至少配置一个");
            return false;
        }
        List<Task> cronTasks = Reflect.on(scheduledTaskRegistrar).get(ShadowJobConstants.DYNAMIC_FIELD_SPRING_CRON_TASKS);
        List<Task> fixedRateTasks = Reflect.on(scheduledTaskRegistrar).get(ShadowJobConstants.DYNAMIC_FIELD_SPRING_FIX_RATE_TASKS);
        List<Task> fixedDelayTasks = Reflect.on(scheduledTaskRegistrar).get(ShadowJobConstants.DYNAMIC_FIELD_SPRING_FIX_DELAY_TASKS);
        return registerTaskWithAllType(shadowJob, key, cronTasks, fixedRateTasks, fixedDelayTasks);
    }

    private boolean registerTaskWithAllType(final ShadowJob shadowJob, String key, List<Task>... tasksArray) {
        boolean registered = false;
        for (List<Task> tasks : tasksArray) {
            registered = registered || registerTaskInternal(shadowJob, key, tasks);
        }
        return registered;
    }

    private boolean registerTaskInternal(final ShadowJob shadowJob, String key, List<Task> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return false;
        }
        for (final Task task : tasks) {
            ScheduledMethodRunnable scheduledMethodRunnable = (ScheduledMethodRunnable) task.getRunnable();
            final String c = scheduledMethodRunnable.getTarget().getClass().getName();
            final String m = scheduledMethodRunnable.getMethod().getName();
            if (!key.equals(task.toString()) && !key.equals(c + "." + m)) {
                continue;
            }
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    // 为了打压测标识
                    Pradar.startTrace(null, c, m);
                    Pradar.setClusterTest(true);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("shadow-job run, className:" + shadowJob.getClassName());
                    }
                    task.getRunnable().run();
                    Pradar.setClusterTest(false);
                    Pradar.endTrace();
                }
            };
            ScheduledFuture<?> schedule;
            if (StringUtils.hasText(shadowJob.getCron())) {
                schedule = threadPoolTaskScheduler.schedule(runnable, new CronTrigger(shadowJob.getCron()));
            } else {
                // 影子任务时间间隔配置
                boolean isFixRate = shadowJob.getFixedRate() != null;
                long interval = isFixRate ? shadowJob.getFixedRate() : shadowJob.getFixedDelay();

                if (shadowJob.getInitialDelay() != null) {
                    Date startTime = new Date(System.currentTimeMillis() + shadowJob.getInitialDelay());
                    schedule = isFixRate ? threadPoolTaskScheduler.scheduleAtFixedRate(runnable, startTime, interval)
                            : threadPoolTaskScheduler.scheduleWithFixedDelay(runnable, startTime, interval);
                } else {
                    schedule = isFixRate ? threadPoolTaskScheduler.scheduleAtFixedRate(runnable, interval)
                            : threadPoolTaskScheduler.scheduleWithFixedDelay(runnable, interval);
                }
            }

            schedulerFuture.put(shadowJob.getClassName(), schedule);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("shadow-job registe success:" + shadowJob.getClassName());
            }
            return true;
        }
        return false;
    }

    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        String key = shaDowJob.getClassName();
        ScheduledFuture scheduledFuture = schedulerFuture.get(key);
        if (null != scheduledFuture) {
            scheduledFuture.cancel(true);
            schedulerFuture.remove(key);
            return true;
        } else {
            shaDowJob.setErrorMessage(key + "未注册or已停止！");
            return false;
        }
    }
}
