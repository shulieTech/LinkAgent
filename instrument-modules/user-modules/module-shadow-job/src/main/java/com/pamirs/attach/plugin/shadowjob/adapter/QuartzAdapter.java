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
//package com.pamirs.attach.plugin.shadowjob.adapter;
//
//import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
//import com.pamirs.attach.plugin.shadowjob.common.quartz.QuartzJobHandlerProvider;
//import com.pamirs.pradar.Pradar;
//import com.pamirs.pradar.pressurement.agent.adapter.JobAdapter;
//import com.pamirs.pradar.internal.config.ShadowJob;
//import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
//import org.quartz.JobDetail;
//import org.quartz.JobKey;
//import org.quartz.JobPersistenceException;
//import org.quartz.Scheduler;
//
///**
// * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
// * @package: com.pamirs.attach.plugin.shadowjob.adapter
// * @Date 2020-03-18 14:38
// */
//public class QuartzAdapter implements JobAdapter {
//
//    @Override
//    public String getJobName() {
//        return "quartz";
//    }
//
//    @Override
//    public boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
//        if (null == PradarSpringUtil.getBeanFactory()) {
//            return false;
//        }
//
//        if (validate(shaDowJob)) {
//            return true;
//        }
//
//        return QuartzJobHandlerProvider.getHandler().registerShadowJob(shaDowJob, null);
//    }
//
//    @Override
//    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
//        return QuartzJobHandlerProvider.getHandler().registerShadowJob(shaDowJob,);
//    }
//
//    private boolean validate(ShadowJob shaDowJob) throws Throwable {
//        Scheduler scheduler = PradarSpringUtil.getBeanFactory().getBean(Scheduler.class);
//        String className = shaDowJob.getClassName();
//        int index = className.lastIndexOf(".");
//        if (-1 != index) {
//            try {
//                JobDetail ptjob = scheduler.getJobDetail(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
//                if (null == ptjob) {
//                    String name = className.substring(0, index + 1) + Pradar.addClusterTestPrefix(className.substring(index + 1));
//                    ptjob = scheduler.getJobDetail(new JobKey(name, ShaDowJobConstant.PLUGIN_GROUP));
//                    if (null == ptjob) {
//                        return false;
//                    }
//                }
//            } catch (Exception e) {
//                if (e instanceof JobPersistenceException) {
//                    scheduler.deleteJob(new JobKey(Pradar.addClusterTestPrefix(className.substring(index + 1)), ShaDowJobConstant.PLUGIN_GROUP));
//                    return false;
//                }
//            }
//
//            return true;
//        }
//        return false;
//    }
//
//}
