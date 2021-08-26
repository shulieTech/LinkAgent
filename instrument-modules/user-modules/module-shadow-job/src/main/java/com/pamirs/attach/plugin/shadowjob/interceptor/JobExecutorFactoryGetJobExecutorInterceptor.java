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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import javax.sql.DataSource;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.schedule.LiteJobFacade;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.pamirs.attach.plugin.shadowjob.common.ElasticJobConfig;
import com.pamirs.attach.plugin.shadowjob.common.ShaDowJobConstant;
import com.pamirs.attach.plugin.shadowjob.common.api.JobAPIFactory;
import com.pamirs.attach.plugin.shadowjob.common.api.JobOperateAPI;
import com.pamirs.attach.plugin.shadowjob.destory.JobDestroy;
import com.pamirs.attach.plugin.shadowjob.obj.PtDataflowJob;
import com.pamirs.attach.plugin.shadowjob.obj.PtElasticJobSimpleJob;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowJob;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * @author angju
 * @date 2021/3/25 13:56
 */
@Destroyable(JobDestroy.class)
public class JobExecutorFactoryGetJobExecutorInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        String jobName = ((LiteJobFacade) args[1]).getShardingContexts().getJobName();
        if (jobName.startsWith("com.pamirs.attach.plugin.shadowjob.obj.PtDataflowJob")
                || jobName.startsWith("com.pamirs.attach.plugin.shadowjob.obj.PtElasticJobSimpleJob")) {
            return advice.getParameterArray();
        }

        if (GlobalConfig.getInstance().getNeedRegisterJobs() != null &&
                GlobalConfig.getInstance().getNeedRegisterJobs().containsKey(jobName) &&
                GlobalConfig.getInstance().getRegisteredJobs() != null &&
                !GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobName)) {
            boolean result = registerShadowJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName));
            if (result) {
                GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName).setActive(0);
                GlobalConfig.getInstance().addRegisteredJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName));
                GlobalConfig.getInstance().getNeedRegisterJobs().remove(jobName);
            }
        }
        if (GlobalConfig.getInstance().getNeedStopJobs() != null &&
                GlobalConfig.getInstance().getNeedStopJobs().containsKey(jobName) &&
                GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobName)) {
            boolean result = disableShaDowJob(GlobalConfig.getInstance().getNeedStopJobs().get(jobName));
            if (result) {
                GlobalConfig.getInstance().getNeedStopJobs().remove(jobName);
                GlobalConfig.getInstance().getRegisteredJobs().remove(jobName);
            }
        }
        return advice.getParameterArray();

    }

    private Field zkConfigField;

    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        if (null != PradarSpringUtil.getBeanFactory()) {
            ZookeeperRegistryCenter registryCenter = PradarSpringUtil.getBeanFactory().getBean(ZookeeperRegistryCenter.class);
            if (null == zkConfigField) {
                zkConfigField = ZookeeperRegistryCenter.class.getDeclaredField("zkConfig");
                zkConfigField.setAccessible(true);
            }
            ZookeeperConfiguration configuration = (ZookeeperConfiguration) zkConfigField.get(registryCenter);

            JobOperateAPI jobOperateAPI = JobAPIFactory.createJobOperateAPI(configuration.getServerLists(), configuration.getNamespace(), configuration.getDigest());
            String className = shaDowJob.getClassName();
            int index = className.lastIndexOf(".");
            if (-1 != index) {
                String ptClassName;
                if (ShaDowJobConstant.SIMPLE.equals(shaDowJob.getJobDataType())) {
                    ptClassName = PtElasticJobSimpleJob.class.getName() + shaDowJob.getClassName();
                } else {
                    ptClassName = PtDataflowJob.class.getName() + shaDowJob.getClassName();
                }
                Collection<String> removeList = jobOperateAPI.remove(ptClassName, null);
                StringBuilder serverIps = new StringBuilder("");
                for (String serverIp : removeList) {
                    Collection<String> remove = jobOperateAPI.remove(ptClassName, serverIp);
                    if (remove.size() > 0) {
                        serverIps.append(remove.toString());
                    }
                }
                if ("".equals(serverIps.toString())) {
                    return true;
                }
                shaDowJob.setErrorMessage(serverIps.toString());
            }

        }
        return true;
    }

    /**
     * 验证影子任务是否已经注册
     *
     * @return
     */
    public boolean checkRegistered(ShadowJob shadowJob) throws NoSuchFieldException, IllegalAccessException {
        Map<String, SpringJobScheduler> schedulerMap = PradarSpringUtil.getBeanFactory().getBeansOfType(SpringJobScheduler.class);
        for (Map.Entry<String, SpringJobScheduler> s : schedulerMap.entrySet()) {
            SpringJobScheduler jobScheduler = s.getValue();
            Field elasticJob = jobScheduler.getClass().getDeclaredField("elasticJob");
            elasticJob.setAccessible(true);
            ElasticJob job = (ElasticJob) elasticJob.get(jobScheduler);
            String ptJobName;
            if (job instanceof SimpleJob) {
                ptJobName = PtElasticJobSimpleJob.class.getName() + shadowJob.getClassName();
            } else {
                ptJobName = PtDataflowJob.class.getName() + shadowJob.getClassName();
            }
            if (ptJobName.equals(job.getClass().getName())) {
                return true;
            }
        }
        return false;
    }


    public boolean registerShadowJob(ShadowJob shadowJob) throws Throwable {
        if (null == PradarSpringUtil.getBeanFactory()) {
            return false;
        }


        try {
            if (checkRegistered(shadowJob)) {
                return true;
            }
            boolean found = false;
            Map<String, SpringJobScheduler> schedulerMap = PradarSpringUtil.getBeanFactory().getBeansOfType(SpringJobScheduler.class);
            for (Map.Entry<String, SpringJobScheduler> s : schedulerMap.entrySet()) {

                SpringJobScheduler jobScheduler = s.getValue();
                Field elasticJob = jobScheduler.getClass().getDeclaredField("elasticJob");
                elasticJob.setAccessible(true);
                ElasticJob job = (ElasticJob) elasticJob.get(jobScheduler);
                if (!job.getClass().getName().equals(shadowJob.getClassName())) {
                    continue;
                }

                found = true;
                JobEventConfiguration jobEventConfig = null;
                try {
                    DataSource dataSource = PradarSpringUtil.getBeanFactory().getBean(DataSource.class);
                    jobEventConfig = new JobEventRdbConfiguration(dataSource);
                } catch (Exception e) {
                    //ignore
                }


                Field liteJobConfigField = JobScheduler.class.getDeclaredField("liteJobConfig");
                liteJobConfigField.setAccessible(true);
                Field regCenterField = JobScheduler.class.getDeclaredField("regCenter");
                regCenterField.setAccessible(true);
                final LiteJobConfiguration liteJobConfig = (LiteJobConfiguration) liteJobConfigField.get(jobScheduler);
                final CoordinatorRegistryCenter registryCenter = (CoordinatorRegistryCenter) regCenterField.get(jobScheduler);

                Class ptJobClass = getPtJobClass(job);

                LOGGER.info("shadow job registering spring className:{}, jobName:{},elasticjob.......", ptJobClass.getName(), job.getClass().getName());

                DefaultListableBeanFactory defaultListableBeanFactory = PradarSpringUtil.getBeanFactory();
                BeanDefinitionBuilder beanSimple = BeanDefinitionBuilder.rootBeanDefinition(ptJobClass);
                defaultListableBeanFactory.registerBeanDefinition(ptJobClass.getSimpleName(), beanSimple.getBeanDefinition());

                LiteJobConfiguration jobConfiguration = null;

                if (ShaDowJobConstant.SIMPLE.equals(shadowJob.getJobDataType())) {
                    jobConfiguration = ElasticJobConfig.createJobConfiguration(ptJobClass, liteJobConfig.getTypeConfig().getCoreConfig().getCron(), liteJobConfig.getTypeConfig().getCoreConfig().getShardingTotalCount(), liteJobConfig.getTypeConfig().getCoreConfig().getShardingItemParameters());
                } else if (ShaDowJobConstant.DATAFLOW.equals(shadowJob.getJobDataType())) {
                    jobConfiguration = ElasticJobConfig.createFlowJobConfiguration(ptJobClass, liteJobConfig.getTypeConfig().getCoreConfig().getCron(), liteJobConfig.getTypeConfig().getCoreConfig().getShardingTotalCount(), liteJobConfig.getTypeConfig().getCoreConfig().getShardingItemParameters());
                } else {
                    throw new IllegalAccessException("【Elastic-Job】" + shadowJob.getJobDataType() + "JobDataType类型错误，未知类型【simple、dataFlow】");
                }
                Field jobNameField = jobConfiguration.getTypeConfig().getCoreConfig().getClass().getDeclaredField("jobName");
                jobNameField.setAccessible(true);
                jobNameField.set(jobConfiguration.getTypeConfig().getCoreConfig(), jobConfiguration.getTypeConfig().getCoreConfig().getJobName() + job.getClass().getName());
                ElasticJobListener[] elasticJobListeners = null;
                if (null != shadowJob.getListenerName() && !"".equals(shadowJob.getListenerName())) {
                    Class<?> listenerClass = Class.forName(shadowJob.getClassName());
                    elasticJobListeners = new ElasticJobListener[]{(ElasticJobListener) listenerClass.newInstance()};
                } else {
                    elasticJobListeners = new ElasticJobListener[0];
                }

                BeanDefinitionBuilder beanDefinitionBuilder = null;
                // 通过BeanDefinitionBuilder创建bean定义
                if (jobEventConfig != null) {
                    beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class)
                            .setInitMethodName("init")
                            .addConstructorArgValue(PradarSpringUtil.getBeanFactory().getBean(ptJobClass.getSimpleName()))
                            .addConstructorArgValue(registryCenter)
                            .addConstructorArgValue(jobConfiguration)
                            .addConstructorArgValue(jobEventConfig)
                            .addConstructorArgValue(elasticJobListeners);
                } else {
                    beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class)
                            .setInitMethodName("init")
                            .addConstructorArgValue(PradarSpringUtil.getBeanFactory().getBean(ptJobClass.getSimpleName()))
                            .addConstructorArgValue(registryCenter)
                            .addConstructorArgValue(jobConfiguration)
                            .addConstructorArgValue(elasticJobListeners);
                }

                String jobBeanName = Pradar.addClusterTestPrefix(job.getClass().getSimpleName()) + SpringJobScheduler.class.getSimpleName();
                // 注册bean
                defaultListableBeanFactory.registerBeanDefinition(jobBeanName, beanDefinitionBuilder.getBeanDefinition());

                SpringJobScheduler ptSpringJobScheduler = (SpringJobScheduler) PradarSpringUtil.getBeanFactory().getBean(jobBeanName);
                Field ptElasticJobField = ptSpringJobScheduler.getClass().getDeclaredField("elasticJob");
                ptElasticJobField.setAccessible(true);
                if (job instanceof SimpleJob) {
                    PtElasticJobSimpleJob ptJob = (PtElasticJobSimpleJob) ptElasticJobField.get(ptSpringJobScheduler);
                    ptJob.setSimpleJob((SimpleJob) job);
                } else {
                    PtDataflowJob ptJob = (PtDataflowJob) ptElasticJobField.get(ptSpringJobScheduler);
                    ptJob.setDataflowJob((DataflowJob) job);
                }
                break;
            }
            if (!found) {
                shadowJob.setErrorMessage("【Elastic-Job】未找到相关Class信息，error:" + shadowJob.getClassName());
                return false;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }

        return true;
    }


    private Class getPtJobClass(ElasticJob job) {
        Class ptJobClass;
        if (job instanceof SimpleJob) {
            ptJobClass = PtElasticJobSimpleJob.class;
        } else {
            ptJobClass = PtDataflowJob.class;
        }
        return ptJobClass;
    }
}
