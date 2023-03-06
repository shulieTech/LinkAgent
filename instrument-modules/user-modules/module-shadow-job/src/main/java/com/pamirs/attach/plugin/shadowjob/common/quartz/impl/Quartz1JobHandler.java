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

import com.pamirs.attach.plugin.shadowjob.common.ClassGeneratorManager;
import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandler;
import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandlerProcessor;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import org.apache.commons.lang.ArrayUtils;
import org.quartz.*;
import org.quartz.impl.StdScheduler;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/23 7:39 下午
 */
public class Quartz1JobHandler implements QuartzJobHandler {
    @Override
    public boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
        Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
//        Object jobGroupNameObj = scheduler.getJobGroupNames();
        Object jobGroupNameObj = scheduler.getClass().getDeclaredMethod("getJobGroupNames").invoke(scheduler);
        String[] jobGroupNames = (String[]) jobGroupNameObj;
        if (ArrayUtils.isEmpty(jobGroupNames)) {
            shaDowJob.setErrorMessage("【Quartz】未找到相关Class信息，error:" + shaDowJob.getClassName());
            return false;
        }

        boolean found = false;
        for (String jobGroupName : jobGroupNames) {
            String[] jobNames = scheduler.getJobNames(jobGroupName);
            if (ArrayUtils.isEmpty(jobNames)) {
                continue;
            }
            for (String registerJobName : jobNames) {
                JobDetail jobDetail = scheduler.getJobDetail(registerJobName, jobGroupName);
                if (jobDetail.getJobClass().getName().equals(shaDowJob.getClassName())) {
                    found = true;

                    Class<? extends Job> jobClass = jobDetail.getJobClass();

                    Trigger[] triggers = ((StdScheduler) scheduler).getTriggersOfJob(registerJobName, jobGroupName);



                    if (null == triggers || triggers.length == 0) {
                        throw new IllegalArgumentException("【Quartz】未找到trigger相关数据");
                    }
                    Trigger trigger = triggers[0];


                    String jobName = Pradar.addClusterTestPrefix(jobClass.getSimpleName());
                    Class quartzClass = null;
                    if ("org.springframework.scheduling.quartz.QuartzJobBean".equals(jobClass.getSuperclass().getName())) {
//                        quartzClass = ClassGeneratorManager.createQuartzClass(Pradar.addClusterTestPrefix(jobClass.getSimpleName()), jobClass.getSimpleName(), jobClass.getPackage().getName(), 1);
                        quartzClass = PtQuartzJobBean.class;
                    } else {
                        quartzClass = ClassGeneratorManager.createQuartzClass(Pradar.addClusterTestPrefix(jobClass.getSimpleName()), jobClass.getSimpleName(), jobClass.getPackage().getName(), 2);
                    }

                    registerJob(shaDowJob.getJobDataType(), quartzClass, jobName, ShaDowJobConstant.PLUGIN_GROUP, trigger , jobClass.getName());
                    scheduler.start();
                    break;
                }
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
            JobDetail jobDetail = new JobDetail(jobName, jobGroupName, jobClass);
            jobDetail.setDescription(busJobClassName);

            if (shadowTrigger instanceof CronTrigger) {

                String cronExpression = ((CronTrigger) shadowTrigger).getCronExpression();
                CronTrigger trigger = new CronTrigger(jobClass.getName(), jobGroupName, cronExpression);
                QuartzJobHandlerProcessor.processShadowJobDelay(trigger);
                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                throw new IllegalArgumentException(String.format("【Quartz】当前类型错误，业务JOB类型: %s, 影子JOB类型: %s", shadowTrigger.getClass(), ShaDowJobConstant.SIMPLE));
            }
        } else if (ShaDowJobConstant.SIMPLE.equals(jobType)) {
            JobDetail jobDetail = new JobDetail(jobName, jobGroupName, jobClass);
            jobDetail.setDescription(busJobClassName);

            // 使用simpleTrigger规则

            if (shadowTrigger instanceof SimpleTrigger) {
                int jobTimes = ((SimpleTrigger) shadowTrigger).getRepeatCount();
                int jobTime = (int) ((SimpleTrigger) shadowTrigger).getRepeatInterval();

                Trigger trigger = null;
                if (jobTimes < 0) {
                    trigger = new SimpleTrigger(jobName, jobGroupName, SimpleTrigger.REPEAT_INDEFINITELY, jobTime);
                } else {
                    trigger = new SimpleTrigger(jobName, jobGroupName, jobTimes, jobTime);
                }
                QuartzJobHandlerProcessor.processShadowJobDelay(trigger);
                scheduler.scheduleJob(jobDetail, trigger);
            } else {
                throw new IllegalArgumentException(String.format("【Quartz】当前类型错误，业务JOB类型: %s, 影子JOB类型: %s", shadowTrigger.getClass(), ShaDowJobConstant.SIMPLE));
            }

        } else {
            throw new IllegalAccessException("【Quartz】" + jobType + "JobDataType类型错误，未知类型【simple、dataFlow】");
        }
    }

    @Override
    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        if (null != PradarSpringUtil.getBeanFactory()) {
            Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
            String className = shaDowJob.getClassName();
            int index = className.lastIndexOf(".");
            if (-1 != index) {
                return scheduler.deleteJob(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP);
            }
            return scheduler.deleteJob(Pradar.addClusterTestPrefix(className), ShaDowJobConstant.PLUGIN_GROUP);

        }
        return false;
    }
}
