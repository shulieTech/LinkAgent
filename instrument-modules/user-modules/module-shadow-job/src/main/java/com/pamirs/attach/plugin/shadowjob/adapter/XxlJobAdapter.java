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
package com.pamirs.attach.plugin.shadowjob.adapter;

import com.pamirs.attach.plugin.shadowjob.interceptor.ProxyInterceptor;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.adapter.JobAdapter;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.shadowjob.adapter
 * @Date 2020-03-18 14:39
 */
public class XxlJobAdapter implements JobAdapter {


    @Override
    public String getJobName() {
        return "xxl-job";
    }

    @Override
    public boolean registerShadowJob(ShadowJob shadowJob) throws Throwable {
        Class<?> handlerClass = Class.forName(shadowJob.getClassName());
        if (null == handlerClass) {
            shadowJob.setErrorMessage("【XXL-JOB】未找到相关Class信息，error:" + shadowJob.getClassName());
            return false;
        }

        String beanName = Pradar.addClusterTestPrefix(handlerClass.getSimpleName());
        Object bean = PradarSpringUtil.getBeanFactory().getBean(handlerClass);
        ProxyInterceptor proxyInterceptor = new ProxyInterceptor(shadowJob, (IJobHandler) bean);
        XxlJobExecutor.registJobHandler(beanName, proxyInterceptor);
        return true;
    }

    private Field jobHandlerRepositoryField;

    @Override
    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        if (null == jobHandlerRepositoryField) {
            jobHandlerRepositoryField = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
            jobHandlerRepositoryField.setAccessible(true);
        }
        ConcurrentMap<String, IJobHandler> map = (ConcurrentMap<String, IJobHandler>) jobHandlerRepositoryField.get(XxlJobExecutor.class);
        String className = shaDowJob.getClassName();
        int lastIndexOfDot = className.lastIndexOf(".");
        if (lastIndexOfDot != -1) {
            className = Pradar.addClusterTestPrefix(className.substring(lastIndexOfDot + 1));
            if (map.keySet().contains(className)) {
                map.remove(className);
                return true;
            }
        }
        return false;
    }
}
