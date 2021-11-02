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
package com.pamirs.attach.plugin.shadowjob.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import com.dangdang.ddframe.job.api.JobScheduler;
import com.dangdang.ddframe.job.api.config.JobConfiguration;
import com.dangdang.ddframe.job.api.config.impl.AbstractJobConfiguration.AbstractJobConfigurationBuilder;
import com.dangdang.ddframe.job.api.config.impl.DataFlowJobConfiguration;
import com.dangdang.ddframe.job.api.config.impl.DataFlowJobConfiguration.DataFlowJobConfigurationBuilder;
import com.dangdang.ddframe.job.api.config.impl.ScriptJobConfiguration;
import com.dangdang.ddframe.job.api.config.impl.SimpleJobConfiguration;
import com.dangdang.ddframe.job.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.internal.executor.JobExecutor;
import com.dangdang.ddframe.job.spring.schedule.SpringJobScheduler;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.PradarInternalService;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public class ElasticJobRegisterUtil {
    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticJobRegisterUtil.class.getName());

    public static void addShadowJob() {
        final DefaultListableBeanFactory beanFactory = PradarSpringUtil.getBeanFactory();
        for (Map.Entry<String, SpringJobScheduler> entry : beanFactory.getBeansOfType(SpringJobScheduler.class)
            .entrySet()) {
            SpringJobScheduler springJobScheduler = entry.getValue();
            JobExecutor jobExecutor = (JobExecutor)ReflectionUtils.getFieldValue(springJobScheduler, "jobExecutor");
            JobConfiguration jobConfiguration = (JobConfiguration)ReflectionUtils.getFieldValues(jobExecutor,
                "schedulerFacade", "configService", "jobNodeStorage", "jobConfiguration");
            JobConfiguration jobConfigurationPT = null;
            final Object elasticJob = jobExecutor.getElasticJob();
            if (jobConfiguration instanceof DataFlowJobConfiguration) {
                final DataFlowJobConfigurationBuilder dataFlowJobConfigurationBuilder
                    = new DataFlowJobConfigurationBuilder(Pradar.addClusterTestPrefix(jobConfiguration.getJobName()),
                    jobConfiguration.getJobClass(),
                    jobConfiguration.getShardingTotalCount(),
                    jobConfiguration.getCron());
                dataFlowJobConfigurationBuilder.concurrentDataProcessThreadCount(
                    ((DataFlowJobConfiguration<?>)jobConfiguration).getConcurrentDataProcessThreadCount());
                dataFlowJobConfigurationBuilder.fetchDataCount(
                    ((DataFlowJobConfiguration<?>)jobConfiguration).getFetchDataCount());
                dataFlowJobConfigurationBuilder.processCountIntervalSeconds(
                    ((DataFlowJobConfiguration<?>)jobConfiguration).getProcessCountIntervalSeconds());
                dataFlowJobConfigurationBuilder.streamingProcess(
                    ((DataFlowJobConfiguration<?>)jobConfiguration).isStreamingProcess());
                fillVar(dataFlowJobConfigurationBuilder, jobConfiguration);
                jobConfigurationPT = dataFlowJobConfigurationBuilder.build();
            } else if (jobConfiguration instanceof ScriptJobConfiguration) {
                // script not need process
                continue;

            } else if (jobConfiguration instanceof SimpleJobConfiguration) {
                jobConfigurationPT = new SimpleJobConfiguration.SimpleJobConfigurationBuilder(
                    Pradar.addClusterTestPrefix(jobConfiguration.getJobName()),
                    jobConfiguration.getJobClass(), jobConfiguration.getShardingTotalCount(),
                    jobConfiguration.getCron()).build();
            } else {
                // 异常情况
                LOGGER.error(String.format("不支持的elasticjob类型:%s", jobConfiguration.toString()));
            }
            List<ElasticJobListener> elasticJobListeners = (List<ElasticJobListener>)ReflectionUtils.getFieldValues(
                jobExecutor,
                "schedulerFacade", "listenerManager", "guaranteeListenerManager", "elasticJobListeners");
            final JobScheduler jobScheduler = new JobScheduler(jobExecutor.getRegCenter(), jobConfigurationPT,
                elasticJobListeners.toArray(new ElasticJobListener[0]));
            PradarSpringUtil.getBeanFactory().registerSingleton(Pradar.addClusterTestPrefix(entry.getKey()),
                jobScheduler);
            JobExecutor ptJobExecutor = (JobExecutor)ReflectionUtils.getFieldValue(jobScheduler, "jobExecutor");
            Class<?> elasticJobClass = null;
            try {
                elasticJobClass = Class.forName("com.dangdang.ddframe.job.api.ElasticJob");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            final Class<?> finalElasticJobClass = elasticJobClass;
            final Object o = Proxy.newProxyInstance(elasticJobClass.getClassLoader(), new Class[] {elasticJobClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("execute")) {
                            String traceId = PradarInternalService.getTraceId();
                            PradarInternalService.startTrace(traceId, null, elasticJob.getClass().getName(), "processData",
                                "elastic-job");
                            PradarInternalService.setClusterTest(true);
                            finalElasticJobClass.getMethod("execute").invoke(elasticJob);
                            PradarInternalService.setClusterTest(false);
                            PradarInternalService.endTrace();
                            return null;
                        } else {
                            return method.invoke(proxy, args);
                        }
                    }
                });
            ReflectionUtils.setFieldValue(ptJobExecutor, "elasticJob", o);
            jobScheduler.init();
        }
    }

    private static void fillVar(AbstractJobConfigurationBuilder abstractJobConfigurationBuilder,
        JobConfiguration jobConfiguration) {
        abstractJobConfigurationBuilder.description("压测 " + jobConfiguration.getDescription());
        abstractJobConfigurationBuilder.disabled(jobConfiguration.isDisabled());
        abstractJobConfigurationBuilder.failover(jobConfiguration.isFailover());
        abstractJobConfigurationBuilder.jobParameter(jobConfiguration.getJobParameter());
        abstractJobConfigurationBuilder.jobShardingStrategyClass(jobConfiguration.getJobShardingStrategyClass());
        abstractJobConfigurationBuilder.maxTimeDiffSeconds(jobConfiguration.getMaxTimeDiffSeconds());
        abstractJobConfigurationBuilder.misfire(jobConfiguration.isMisfire());
        abstractJobConfigurationBuilder.monitorExecution(jobConfiguration.isMonitorExecution());
        abstractJobConfigurationBuilder.monitorPort(jobConfiguration.getMonitorPort());
        abstractJobConfigurationBuilder.overwrite(jobConfiguration.isOverwrite());
        abstractJobConfigurationBuilder.shardingItemParameters(jobConfiguration.getShardingItemParameters());
    }
}
