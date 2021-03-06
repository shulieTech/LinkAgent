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
package com.pamirs.attach.plugin.shadowjob.common.quartz.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandler;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtJob;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import org.apache.commons.lang.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 7:39 下午
 */
public class Quartz2JobHandler implements QuartzJobHandler {
    private final static Logger logger = LoggerFactory.getLogger(Quartz2JobHandler.class.getName());

    @Override
    public boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
        Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.<JobKey>anyGroup());
        if (StringUtils.isBlank(shaDowJob.getListenerName()) || !shaDowJob.getListenerName().contains(".")){
            logger.error("quartz job ListenerName is not current,ListenerName need set bus job triggerName.groupName");
            return false;
        }
        String[] triggerAndGroup = shaDowJob.getListenerName().split("\\.");
        boolean found = false;
        for (JobKey jobKey : jobKeys) {
            Object jobDetail = scheduler.getJobDetail(jobKey);
            Method method = jobDetail.getClass().getDeclaredMethod("getJobClass");
            Class jobClass = (Class) method.invoke(jobDetail);
            if (jobClass.getName().equals(shaDowJob.getClassName())) {
                found = true;

                Trigger trigger = scheduler.getTrigger(new TriggerKey(triggerAndGroup[0], triggerAndGroup[1]));


                if (null == trigger) {
                    throw new IllegalArgumentException("【Quartz】未找到trigger相关数据");
                }

                String jobName = Pradar.addClusterTestPrefix(jobClass.getSimpleName());
                Class quartzClass = null;
                if ("org.springframework.scheduling.quartz.QuartzJobBean".equals(jobClass.getSuperclass().getName())) {
                    quartzClass = PtQuartzJobBean.class;
                } else {
                    quartzClass = PtJob.class;//ClassGeneratorManager.createQuartzClass(Pradar.addClusterTestPrefix(jobClass.getSimpleName()), jobClass.getSimpleName(), jobClass.getPackage().getName(), 2);
                }

                registerJob(shaDowJob.getJobDataType(), quartzClass, jobName, ShaDowJobConstant.PLUGIN_GROUP, trigger, shaDowJob.getClassName());
                scheduler.start();
                break;
            }
        }

        if (!found) {
            shaDowJob.setErrorMessage("【Quartz】未找到相关Class信息，error:" + shaDowJob.getClassName());
            return false;
        }
        return true;
    }

    private void registerJob(String jobType, Class jobClass, String jobName,
                             String jobGroupName, Trigger shadowTrigger, String busJobClassName) throws Exception {
        Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
        if (ShaDowJobConstant.DATAFLOW.equals(jobType)) {

            JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, jobGroupName).build();
            setDescription(jobDetail, busJobClassName);
            if (shadowTrigger instanceof CronTriggerImpl) {

                String cronExpression = ((CronTriggerImpl) shadowTrigger).getCronExpression();
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
                Method getRepeatIntervalMethod = shadowTrigger.getClass().getDeclaredMethod("getRepeatInterval");
                Long jobTime = (Long) getRepeatIntervalMethod.invoke(shadowTrigger);


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

    private void setDescription(JobDetail jobDetail, String busJobClassName){
        Field field = null;
        try {
            field = jobDetail.getClass().getDeclaredField("description");
            field.setAccessible(true);
            field.set(jobDetail, busJobClassName);
        }catch (Exception e){

        }finally {
            if (field != null){
                field.setAccessible(false);
            }
        }

    }

    @Override
    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        if (null != PradarSpringUtil.getBeanFactory()) {
            Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
            String className = shaDowJob.getClassName();
            int index = className.lastIndexOf(".");
            if (-1 != index) {
                return scheduler.deleteJob(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
            }

        }
        return false;
    }
}
