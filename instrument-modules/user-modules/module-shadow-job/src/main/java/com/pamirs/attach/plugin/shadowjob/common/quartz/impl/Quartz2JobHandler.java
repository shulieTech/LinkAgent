/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.shadowjob.common.quartz.impl;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandler;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtJob;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 7:39 下午
 */
public class Quartz2JobHandler implements QuartzJobHandler {
    private final static Logger logger = LoggerFactory.getLogger(Quartz2JobHandler.class.getName());

    private Scheduler scheduler;

    public boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
        boolean found = false;
        String jobDataType = shaDowJob.getJobDataType();

        if (ShaDowJobConstant.SIMPLE.equals(jobDataType)) {
            Map<String, SimpleTriggerFactoryBean> simpleTriggerFactoryBeanMap = PradarSpringUtil.getBeanFactory().getBeansOfType(SimpleTriggerFactoryBean.class);
            for (Map.Entry entry : simpleTriggerFactoryBeanMap.entrySet()) {
                SimpleTriggerFactoryBean factoryBean = (SimpleTriggerFactoryBean) entry.getValue();
                Object jobDetail = ReflectionUtils.get(factoryBean, "jobDetail");
                Class jobClass = ReflectionUtils.invoke(jobDetail, "getJobClass");
                if (jobClass.getName().contains("MethodInvokingJobDetailFactoryBean")) {
                    JobDataMap jobDataMap = ReflectionUtils.get(jobDetail, "jobDataMap");
                    jobClass = ReflectionUtils.get(jobDataMap.get("methodInvoker"), "targetClass");
                }
                if (!jobClass.getName().equals(shaDowJob.getClassName())) {
                    continue;
                }

                Class quartzClass;
                if ("org.springframework.scheduling.quartz.QuartzJobBean".equals(jobClass.getSuperclass().getName())) {
                    quartzClass = PtQuartzJobBean.class;
                } else {
                    quartzClass = PtJob.class;
                }

                String jobName = Pradar.addClusterTestPrefix(jobClass.getSimpleName());
                Trigger trigger = ReflectionUtils.get(factoryBean, "simpleTrigger");

                registerJob(shaDowJob.getJobDataType(), quartzClass, jobName, ShaDowJobConstant.PLUGIN_GROUP, trigger, shaDowJob.getClassName(), shaDowJob);
                return true;
            }
        } else if (ShaDowJobConstant.DATAFLOW.equals(jobDataType)) {

            Map<String, CronTriggerFactoryBean> cronTriggerFactoryBean = PradarSpringUtil.getBeanFactory().getBeansOfType(CronTriggerFactoryBean.class);

            for (Map.Entry entry : cronTriggerFactoryBean.entrySet()) {
                CronTriggerFactoryBean factoryBean = (CronTriggerFactoryBean) entry.getValue();

                Object jobDetail = ReflectionUtils.get(factoryBean, "jobDetail");
                Class jobClass = ReflectionUtils.invoke(jobDetail, "getJobClass");
                if (jobClass.getName().contains("MethodInvokingJobDetailFactoryBean")) {
                    JobDataMap jobDataMap = ReflectionUtils.get(jobDetail, "jobDataMap");
                    jobClass = ReflectionUtils.get(jobDataMap.get("methodInvoker"), "targetClass");
                }
                if (!jobClass.getName().equals(shaDowJob.getClassName())) {
                    continue;
                }

                Class quartzClass;
                if ("org.springframework.scheduling.quartz.QuartzJobBean".equals(jobClass.getSuperclass().getName())) {
                    quartzClass = PtQuartzJobBean.class;
                } else {
                    quartzClass = PtJob.class;
                }

                String jobName = Pradar.addClusterTestPrefix(jobClass.getSimpleName());
                Trigger trigger = ReflectionUtils.get(factoryBean, "cronTrigger");

                registerJob(shaDowJob.getJobDataType(), quartzClass, jobName, ShaDowJobConstant.PLUGIN_GROUP, trigger, shaDowJob.getClassName(), shaDowJob);
                return true;
            }

        }

        if (!found) {
            shaDowJob.setErrorMessage("【Quartz】未找到相关Class信息，error:" + shaDowJob.getClassName());
        }
        return false;
    }

    private void registerJob(String jobType, Class jobClass, String jobName,
                             String jobGroupName, Trigger shadowTrigger, String busJobClassName, ShadowJob shaDowJob) throws Exception {

        Scheduler scheduler;
        try {
            scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
        } catch (Exception e) {
            if (this.scheduler == null) {
                this.scheduler = (Scheduler) SyncObjectService.getSyncObject("org.quartz.impl.StdScheduler").getDatas().get(0).getTarget();
            }
            scheduler = this.scheduler;
        }

        if (ShaDowJobConstant.DATAFLOW.equals(jobType)) {

            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
            setDescription(jobDetail, busJobClassName);
            if (shadowTrigger instanceof CronTriggerImpl) {

                String cronExpression = shaDowJob.getCron();
                //表达式调度构建器(即任务执行的时间)
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);

                //按新的cronExpression表达式构建一个新的trigger
                CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobClass.getName(), jobGroupName)
                        .withSchedule(scheduleBuilder).build();

                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                throw new IllegalArgumentException(String.format("【Quartz】当前类型错误，业务JOB类型: %s, 影子JOB类型: %s", shadowTrigger.getClass(), ShaDowJobConstant.SIMPLE));
            }
        } else if (ShaDowJobConstant.SIMPLE.equals(jobType)) {

            // 任务名称和组构成任务key
            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName)
                    .build();
            setDescription(jobDetail, busJobClassName);
            // 使用simpleTrigger规则
            Trigger trigger = null;
            if (shadowTrigger instanceof SimpleTriggerImpl) {
                int jobTimes = ((SimpleTriggerImpl) shadowTrigger).getRepeatCount();
                // 没取到执行次数默认-1,即不限次数
                if(jobTimes == 0){
                    jobTimes = -1;
                }
                Method getRepeatIntervalMethod = shadowTrigger.getClass().getDeclaredMethod("getRepeatInterval");
                Long jobTime = (Long) getRepeatIntervalMethod.invoke(shadowTrigger);

                if (jobTime == null || jobTime == 0) {
                    CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(shaDowJob.getCron());
                    CronExpression cronExpression = ReflectionUtils.get(scheduleBuilder, "cronExpression");
                    Date nextFireTime = cronExpression.getNextValidTimeAfter(new Date());
                    Date nextValidTimeAfter = cronExpression.getNextValidTimeAfter(nextFireTime);
                    jobTime = nextValidTimeAfter.getTime() - nextFireTime.getTime();
                }

                if (jobTimes < 0) {
                    trigger = TriggerBuilder.newTrigger().withIdentity(jobName, jobGroupName)
                            .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(1).withIntervalInSeconds(jobTime.intValue()))
                            .startNow().build();
                } else {
                    trigger = TriggerBuilder
                            .newTrigger().withIdentity(jobName, jobGroupName).withSchedule(SimpleScheduleBuilder
                                    .repeatSecondlyForever(1).withIntervalInSeconds(jobTime.intValue() / 1000).withRepeatCount(jobTimes))
                            .startNow().build();
                }
                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                throw new IllegalArgumentException(String.format("【Quartz】当前类型错误，业务JOB类型: %s, 影子JOB类型: %s", shadowTrigger.getClass(), ShaDowJobConstant.SIMPLE));
            }

        } else {
            throw new IllegalAccessException("【Quartz】" + jobType + "JobDataType类型错误，未知类型【simple、dataFlow】");
        }
    }

    private void setDescription(JobDetail jobDetail, String busJobClassName) {
        Field field = null;
        try {
            field = jobDetail.getClass().getDeclaredField("description");
            field.setAccessible(true);
            field.set(jobDetail, busJobClassName);
        } catch (Exception e) {

        } finally {
            if (field != null) {
                field.setAccessible(false);
            }
        }

    }

    @Override
    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        if (null != PradarSpringUtil.getBeanFactory()) {
            Scheduler scheduler = this.scheduler;
            if (scheduler == null) {
                scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
            }
            String className = shaDowJob.getClassName();
            int index = className.lastIndexOf(".");
            if (-1 != index) {
                return scheduler.deleteJob(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
            }

        }
        return false;
    }
}
