/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.shadowjob.interceptor;

import com.pamirs.attach.plugin.shadowjob.adapter.XxlJobAdapter;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.StringUtil;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/4/25 3:44 下午
 */
public class XxlOptionInterceptor extends AroundInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(XxlOptionInterceptor.class);

    private Field jobHandlerRepositoryField;

    @Override
    public void doBefore(Advice advice) {
        //  将delay=0去掉
        try {
            XxlJobAdapter xxlJobAdapter = new XxlJobAdapter();
            if (GlobalConfig.getInstance().getJobAdaptor(xxlJobAdapter.getJobName()) == null) {
                GlobalConfig.getInstance().addJobAdaptor(xxlJobAdapter.getJobName(), xxlJobAdapter);
            }
        } catch (Throwable t) {
            //ignore
        }
        registerShadowJob();
        disableShadowJob();
    }

    private void registerShadowJob() {
        try {
            if (XxlJobAdapter.needRegisterMap.isEmpty()) {
                return;
            }
            for (Map.Entry<String, ShadowJob> entry : XxlJobAdapter.needRegisterMap.entrySet()) {
                try {
                    registerShadowJob0(entry.getValue());
                    XxlJobAdapter.registerSuccessMap.put(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    // ignore
                }
            }
            removeAll(XxlJobAdapter.needRegisterMap, XxlJobAdapter.registerSuccessMap);
            removeAll(XxlJobAdapter.disableSuccessMap, XxlJobAdapter.registerSuccessMap);
        } catch (Exception e) {
            logger.error("xxljob registerShadowJob error.", e);
        }
    }

    private void disableShadowJob() {
        try {
            if (XxlJobAdapter.needDisableMap.isEmpty()) {
                return;
            }
            for (Map.Entry<String, ShadowJob> entry : XxlJobAdapter.needDisableMap.entrySet()) {
                try {
                    disableShadowJob0(entry.getValue());
                    XxlJobAdapter.disableSuccessMap.put(entry.getKey(), entry.getValue());
                } catch (Exception e) {
                    // ignore
                }
            }
            removeAll(XxlJobAdapter.needDisableMap, XxlJobAdapter.disableSuccessMap);
            removeAll(XxlJobAdapter.registerSuccessMap, XxlJobAdapter.disableSuccessMap);
        } catch (Exception e) {
            logger.error("xxljob disableShadowJob error.", e);
        }
    }

    private void registerShadowJob0(ShadowJob shadowJob) throws NoSuchFieldException, IllegalAccessException {
        String taskName = getTaskName(shadowJob.getClassName());
        if (StringUtil.isEmpty(taskName)) {
            return;
        }

        ConcurrentMap<String, IJobHandler> jobHandlerRepository = getJobHandlerRepositoryMap();
        if (jobHandlerRepository == null) {
            return;
        }

        Object bean = jobHandlerRepository.get(taskName);
        if (bean == null) {
            return;
        }

        ProxyInterceptor proxyInterceptor = new ProxyInterceptor(shadowJob, (IJobHandler) bean);
        XxlJobExecutor.registJobHandler(Pradar.addClusterTestPrefix(taskName), proxyInterceptor);
    }

    private void disableShadowJob0(ShadowJob shadowJob) throws Exception {
        ConcurrentMap<String, IJobHandler> map = getJobHandlerRepositoryMap();
        if (map == null) {
            return;
        }

        String taskName = getTaskName(shadowJob.getClassName());
        if (StringUtil.isEmpty(taskName)) {
            return;
        }

        taskName = Pradar.addClusterTestPrefix(taskName);
        map.remove(taskName);
    }

    private void removeAll(Map<String, ShadowJob> origin, Map<String, ShadowJob> removeData) {
        if (origin.isEmpty() || removeData.isEmpty()) {
            return;
        }
        for (Map.Entry entry : removeData.entrySet()) {
            origin.remove(entry.getKey());
        }
    }

    /**
     * 获取 XxlJobExecutor 的 jobHandlerRepository 字段，xxljob所有的任务都注册在这上面
     *
     * @return ConcurrentMap
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private ConcurrentMap<String, IJobHandler> getJobHandlerRepositoryMap() throws NoSuchFieldException, IllegalAccessException {
        if (null == jobHandlerRepositoryField) {
            jobHandlerRepositoryField = XxlJobExecutor.class.getDeclaredField("jobHandlerRepository");
            jobHandlerRepositoryField.setAccessible(true);
        }
        if (jobHandlerRepositoryField == null) {
            return null;
        }
        return (ConcurrentMap<String, IJobHandler>) jobHandlerRepositoryField.get(XxlJobExecutor.class);
    }


    /**
     * 根据控制台配置的 taskClassName 来获取对应的jobName。
     * ps:正确做法应该是修改ShadowJob的属性，但是ShadowJob在pradarCore模块中定义，目前暂时不去修改pradarCore模块，所以这里特殊处理一下
     *
     * @param taskClassName xxlJob的类名
     * @return xxlJob的任务名
     */
    private String getTaskName(String taskClassName) {
        if (StringUtil.isEmpty(taskClassName)) {
            return null;
        }
        String taskName;
        int index = taskClassName.lastIndexOf(".");
        if (index != -1) {
            // 情况一：用户配置了类全名
            taskName = toLowerCaseFirstOne(taskClassName.substring(index + 1));
        } else {
            // 情况二：用户配置了job对象名
            taskName = taskClassName;
        }
        return taskName;
    }

    /**
     * 工具方法：将字符串首字母改成小写
     *
     * @param s 字符串
     * @return 首字母小写字符串
     */
    private String toLowerCaseFirstOne(String s) {
        if (StringUtil.isEmpty(s)) {
            return null;
        }
        if (Character.isLowerCase(s.charAt(0))) {
            return s;
        } else {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
    }
}
