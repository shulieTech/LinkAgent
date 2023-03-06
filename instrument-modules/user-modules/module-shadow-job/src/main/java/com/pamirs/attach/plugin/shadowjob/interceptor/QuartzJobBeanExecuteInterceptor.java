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

import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandlerProcessor;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.quartz.*;

/**
 * @author angju
 * @date 2021/3/26 16:23
 */
public class QuartzJobBeanExecuteInterceptor extends ParametersWrapperInterceptorAdaptor {
    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        JobExecutionContext jobExecutionContext = (JobExecutionContext) args[0];
        String jobClassName = jobExecutionContext.getJobDetail().getJobClass().getName();
        if (GlobalConfig.getInstance().getNeedRegisterJobs() != null &&
                GlobalConfig.getInstance().getNeedRegisterJobs().containsKey(jobClassName) &&
                GlobalConfig.getInstance().getRegisteredJobs() != null &&
                !GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobClassName)){
            boolean result = registerShadowJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobClassName));
            if (result){
                GlobalConfig.getInstance().getNeedRegisterJobs().get(jobClassName).setActive(0);
                GlobalConfig.getInstance().addRegisteredJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobClassName));
                GlobalConfig.getInstance().getNeedRegisterJobs().remove(jobClassName);
            }
        }
        if (GlobalConfig.getInstance().getNeedStopJobs() != null &&
                GlobalConfig.getInstance().getNeedStopJobs().containsKey(jobClassName) &&
                GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobClassName)){
            boolean result = disableShaDowJob(GlobalConfig.getInstance().getNeedStopJobs().get(jobClassName));
            if (result){
                GlobalConfig.getInstance().getNeedStopJobs().remove(jobClassName);
                GlobalConfig.getInstance().getRegisteredJobs().remove(jobClassName);
            }
        }
        return advice.getParameterArray();
    }

    private boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
        if (null == PradarSpringUtil.getBeanFactory()) {
            return false;
        }

        if (validate(shaDowJob)) {
            return true;
        }

        return QuartzJobHandlerProcessor.getHandler().registerShadowJob(shaDowJob);
    }


    private boolean validate(ShadowJob shaDowJob) throws Throwable {
        Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
        String className = shaDowJob.getClassName();
        int index = className.lastIndexOf(".");
        if (-1 != index) {
            try {
//                JobDetail ptjob = scheduler.getJobDetail(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
                JobDetail ptjob = scheduler.getJobDetail(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP);
                if (null == ptjob) {
                    String name = className.substring(0, index + 1) + Pradar.addClusterTestPrefix(className.substring(index + 1));
                    ptjob = scheduler.getJobDetail(name, ShaDowJobConstant.PLUGIN_GROUP);
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
        return QuartzJobHandlerProcessor.getHandler().disableShaDowJob(shaDowJob);
    }
}
