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
package com.pamirs.attach.plugin.shadowjob.interceptor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandlerProvider;
import com.pamirs.attach.plugin.shadowjob.destory.JobDestroy;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtJob;
import com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ResultInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.quartz.*;

/**
 * @author angju
 * @date 2021/3/28 20:21
 */
@Destroyable(JobDestroy.class)
public class JobRunShellInitializeInterceptor extends ResultInterceptorAdaptor {
    private Field jecField;
    private Field jobField;
    private Field jobDetailField;
    private Field jobDescriptionField;

    private static final Map<String, Job> stringJobMap = new ConcurrentHashMap<String, Job>(16, 1);

    @Override
    public Object getResult0(Advice advice) {
        Object target = advice.getTarget();

        try {
            if (jecField == null) {
                jecField = target.getClass().getDeclaredField("jec");
            }

            jecField.setAccessible(true);
            Object jec = jecField.get(target);
            if (jobField == null) {
                jobField = jec.getClass().getDeclaredField("job");
            }
            jobField.setAccessible(true);
            Job busJob = (Job) jobField.get(jec);

            String busJobClassName = busJob.getClass().getName();

            if (busJobClassName.contains("MethodInvokingJobDetailFactoryBean")) {
                JobDataMap jobDataMap = ReflectionUtils.get(jec, "jobDataMap");
                Class jobClass = ReflectionUtils.get(jobDataMap.get("methodInvoker"), "targetClass");
                busJobClassName = jobClass.getName();

            } else if (busJobClassName.equals("com.pamirs.attach.plugin.shadowjob.obj.quartz.PtJob")) {
                if (((PtJob) ((busJob))).getBusJob() == null) {
                    if (jobDetailField == null) {
                        jobDetailField = jec.getClass().getDeclaredField("jobDetail");
                        jobDetailField.setAccessible(true);
                    }
                    Object jobDetail = jobDetailField.get(jec);
                    if (jobDescriptionField == null) {
                        jobDescriptionField = jobDetail.getClass().getDeclaredField("description");
                        jobDescriptionField.setAccessible(true);
                    }
                    String key = (String) jobDescriptionField.get(jobDetail);
                    ((PtJob) ((busJob))).setBusJob(stringJobMap.get(key));
                }
                return advice.getReturnObj();
            } else if (busJobClassName.equals("com.pamirs.attach.plugin.shadowjob.obj.quartz.PtQuartzJobBean")) {
                if (((PtQuartzJobBean) ((busJob))).getBusJob() == null) {
                    if (jobDetailField == null) {
                        jobDetailField = jec.getClass().getDeclaredField("jobDetail");
                        jobDetailField.setAccessible(true);
                    }
                    Object jobDetail = jobDetailField.get(jec);
                    if (jobDescriptionField == null) {
                        jobDescriptionField = jobDetail.getClass().getDeclaredField("description");
                        jobDescriptionField.setAccessible(true);
                    }
                    String key = (String) jobDescriptionField.get(jobDetail);
                    ((PtQuartzJobBean) ((busJob))).setBusJob(stringJobMap.get(key));
                }
                return advice.getReturnObj();
            }

            String jobClassName = busJobClassName;
            if (GlobalConfig.getInstance().getNeedRegisterJobs() != null &&
                    GlobalConfig.getInstance().getNeedRegisterJobs().containsKey(jobClassName) &&
                    GlobalConfig.getInstance().getRegisteredJobs() != null &&
                    !GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobClassName)) {
                if (!stringJobMap.containsKey(jobClassName)) {
                    stringJobMap.put(jobClassName, busJob);
                }
                boolean result = registerShadowJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobClassName));
                if (result) {
                    GlobalConfig.getInstance().getNeedRegisterJobs().get(jobClassName).setActive(0);
                    GlobalConfig.getInstance().addRegisteredJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobClassName));
                    GlobalConfig.getInstance().getNeedRegisterJobs().remove(jobClassName);
                }
            }
            if (GlobalConfig.getInstance().getNeedStopJobs() != null &&
                    GlobalConfig.getInstance().getNeedStopJobs().containsKey(jobClassName) &&
                    GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobClassName)) {
                stringJobMap.remove(jobClassName);
                boolean result = disableShaDowJob(GlobalConfig.getInstance().getNeedStopJobs().get(jobClassName));
                if (result) {
                    GlobalConfig.getInstance().getNeedStopJobs().remove(jobClassName);
                    GlobalConfig.getInstance().getRegisteredJobs().remove(jobClassName);
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("JobRunShellInitializeInterceptor error {}", e);
        }


        return advice.getReturnObj();
    }


    private boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
        if (null == PradarSpringUtil.getBeanFactory()) {
            return false;
        }

        if (validate(shaDowJob)) {
            return true;
        }

        return QuartzJobHandlerProvider.getHandler().registerShadowJob(shaDowJob);
    }


    private boolean validate(ShadowJob shaDowJob) throws Throwable {
        Scheduler scheduler;
        try {
            scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
        } catch (Throwable throwable) {
            return false;
        }

        String className = shaDowJob.getClassName();
        int index = className.lastIndexOf(".");
        if (-1 != index) {
            try {
                JobDetail ptjob;
                try {
                    //2.x用这个方法
                    ptjob = scheduler.getJobDetail(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
                } catch (Throwable e) {
                    //1.x用这个方法
                    ptjob = scheduler.getJobDetail(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP);
                }


                if (null == ptjob) {
                    String name = className.substring(0, index + 1) + Pradar.addClusterTestPrefix(className.substring(index + 1));

                    try {
                        ptjob = scheduler.getJobDetail(new JobKey(name, ShaDowJobConstant.PLUGIN_GROUP));
                    } catch (Throwable e) {
                        ptjob = scheduler.getJobDetail(name, ShaDowJobConstant.PLUGIN_GROUP);
                    }
                    if (null == ptjob) {
                        return false;
                    }
                }
            } catch (Exception e) {
                if (e instanceof JobPersistenceException) {
                    scheduler.deleteJob(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
                    return false;
                }
            }

            return true;
        }
        return false;
    }


    private boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        return QuartzJobHandlerProvider.getHandler().disableShaDowJob(shaDowJob);
    }
}
