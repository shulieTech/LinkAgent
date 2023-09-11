package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.attach.plugin.shadowjob.ShadowJobConstants;
import com.pamirs.attach.plugin.shadowjob.shadowRunnable.PradarShadowJobRunnable;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/10/18 14:34
 */
public class ReschedulingRunnableInterceptor extends ParametersWrapperInterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ReschedulingRunnableInterceptor.class);

    private final Map<String, ScheduledTask> schedulerFuture = new HashMap<String, ScheduledTask>();

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        Runnable runnable = Reflect.on(advice.getTarget()).get("delegate");
        if (runnable instanceof PradarShadowJobRunnable) {
            return advice.getParameterArray();
        }
        String key = runnable.getClass().getName();
        ShadowJob needRegisterShadowJob = needRegister(key);
        if (needRegisterShadowJob != null) {
            boolean result = registerShadowJob(needRegisterShadowJob);
            if (result) {
                needRegisterShadowJob.setActive(0);
                GlobalConfig.getInstance().addRegisteredJob(needRegisterShadowJob);
                GlobalConfig.getInstance().getNeedRegisterJobs().remove(needRegisterShadowJob.getClassName());
            }
        }

        ShadowJob needStopShadowJob = needStop(key);
        if (needStopShadowJob != null) {
            boolean result = disableShaDowJob(needStopShadowJob);
            if (result) {
                GlobalConfig.getInstance().getNeedStopJobs().remove(needStopShadowJob.getClassName());
                GlobalConfig.getInstance().getRegisteredJobs().remove(needStopShadowJob.getClassName());
            }
        }

        return advice.getParameterArray();
    }

    @Override
    protected void clean() {
        for (Map.Entry<String, ScheduledTask> entry : schedulerFuture.entrySet()) {
            entry.getValue().cancel();
        }
        schedulerFuture.clear();
    }

    /**
     * 注册影子jo，把以某个前缀匹配的定时任务全部注册上去b
     *
     * @param needRegisterShadowJob 需要注册的job对象
     * @return true or false
     * @throws Throwable throwable
     */
    public boolean registerShadowJob(ShadowJob needRegisterShadowJob) throws Throwable {
        if (PradarSpringUtil.getBeanFactory() == null) {
            LOGGER.warn("PradarSpringUtil.getBeanFactory is null,can not register shaDowJob： " + needRegisterShadowJob);
            return false;
        }

        // 通过springboot applicationContext找到注册的job
        ScheduledAnnotationBeanPostProcessor bean = PradarSpringUtil.getBeanFactory().getBean(ScheduledAnnotationBeanPostProcessor.class);
        Field registrar = ScheduledAnnotationBeanPostProcessor.class.getDeclaredField("registrar");
        registrar.setAccessible(true);
        ScheduledTaskRegistrar scheduledTaskRegistrar = (ScheduledTaskRegistrar) registrar.get(bean);
        List<TriggerTask> triggerTasks =
                Reflect.on(scheduledTaskRegistrar).get(ShadowJobConstants.DYNAMIC_FIELD_SPRING_TRIGGER_TASKS);
        // 从业务应用注册的schedule任务找到需要注册的影子job
        boolean registeRes = false;
        for (final TriggerTask triggerTask : triggerTasks) {
            Runnable runnable = triggerTask.getRunnable();
            if (runnable instanceof PradarShadowJobRunnable) {
                continue;
            }
            String className = runnable.getClass().getName();
            if (className.startsWith(needRegisterShadowJob.getClassName())) {
                // 注册影子job
                Trigger trigger = triggerTask.getTrigger();
                Runnable shadowRunnable = new PradarShadowJobRunnable(runnable, className, "triggerTask");
                TriggerTask shadowTriggerTask = new TriggerTask(shadowRunnable, trigger);

                ScheduledTask schedule = scheduledTaskRegistrar.scheduleTriggerTask(shadowTriggerTask);
                schedulerFuture.put(className, schedule);
                registeRes = true;
            }
        }
        if (!registeRes) {
            logger.error("未找到应用已注册真实job:" + needRegisterShadowJob);
        }
        return registeRes;
    }

    /**
     * 销毁影子job
     *
     * @param needStopShadowJob 需要停止的job
     * @return true or false
     * @throws Throwable throwable
     */
    public boolean disableShaDowJob(ShadowJob needStopShadowJob) throws Throwable {
        Iterator<Map.Entry<String, ScheduledTask>> iterator = schedulerFuture.entrySet().iterator();
        boolean disabled = false;
        while (iterator.hasNext()) {
            Map.Entry<String, ScheduledTask> item = iterator.next();
            if (item.getKey().startsWith(needStopShadowJob.getClassName())) {
                item.getValue().cancel();
                iterator.remove();
                disabled = true;
            }
        }
        return disabled;
    }


    /**
     * 判断是否需要注册
     *
     * @param key key
     * @return true or false
     */
    private ShadowJob needRegister(String key) {
        boolean needRegisterMapIsNull = GlobalConfig.getInstance().getNeedRegisterJobs() == null;
        if (needRegisterMapIsNull) {
            return null;
        }

        boolean registered = schedulerFuture.containsKey(key);
        if (registered) {
            return null;
        }

        // 如果类是用lambda创建的，得到的类名会带上 '$',所以只需要判断类名前缀是否匹配即可
        for (Map.Entry<String, ShadowJob> entry : GlobalConfig.getInstance().getNeedRegisterJobs().entrySet()) {
            if (key.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * 判断是否需要停止
     *
     * @param key key
     * @return true or false
     */
    private ShadowJob needStop(String key) {
        boolean needStopMapIsNull = GlobalConfig.getInstance().getNeedStopJobs() == null;
        if (needStopMapIsNull) {
            return null;
        }

        boolean registered = schedulerFuture.containsKey(key);
        if (!registered) {
            return null;
        }

        // 如果类是用lambda创建的，得到的类名会带上 '$',所以只需要判断类名前缀是否匹配即可
        for (Map.Entry<String, ShadowJob> entry : GlobalConfig.getInstance().getNeedStopJobs().entrySet()) {
            if (key.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }
}
