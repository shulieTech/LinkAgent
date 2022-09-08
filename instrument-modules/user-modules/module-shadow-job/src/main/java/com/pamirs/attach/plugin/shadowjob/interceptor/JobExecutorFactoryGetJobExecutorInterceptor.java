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

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.JobType;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.dangdang.elasticjob.lite.annotation.ElasticSimpleJob;
import com.dangdang.elasticjob.lite.autoconfigure.ElasticJobAutoConfiguration;
import com.pamirs.attach.plugin.shadowjob.cache.ElasticJobCache;
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
import com.shulie.instrument.simulator.api.ThrowableUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author angju
 * @date 2021/3/25 13:56
 */
@Destroyable(JobDestroy.class)
public class JobExecutorFactoryGetJobExecutorInterceptor extends ParametersWrapperInterceptorAdaptor {

    Logger logger = LoggerFactory.getLogger(getClass());
    Set registered = new HashSet();

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        ElasticJobCache.bizClassLoad = advice.getTargetClass().getClassLoader();
        Object[] args = advice.getParameterArray();
//        String jobName = ((LiteJobFacade) args[1]).getShardingContexts().getJobName();
        String jobName = args[0].getClass().getName();
        if (jobName.startsWith("com.pamirs.attach.plugin.shadowjob.obj.PtDataflowJob")
                || jobName.startsWith("com.pamirs.attach.plugin.shadowjob.obj.PtElasticJobSimpleJob")) {
            return advice.getParameterArray();
        }
        if (PradarSpringUtil.getBeanFactory() != null) {
            ElasticJobCache.registryCenter = PradarSpringUtil.getBeanFactory().getBean(ZookeeperRegistryCenter.class);
        }

        if (GlobalConfig.getInstance().getNeedRegisterJobs() != null &&
                GlobalConfig.getInstance().getNeedRegisterJobs().containsKey(jobName) &&
                GlobalConfig.getInstance().getRegisteredJobs() != null &&
                !GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobName)) {
            ShadowJob shadowJob = GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName);
            boolean result = registerShadowJob(shadowJob);
            if (result) {
                GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName).setActive(0);
                GlobalConfig.getInstance().addRegisteredJob(GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName));
                GlobalConfig.getInstance().getNeedRegisterJobs().remove(jobName);
                ElasticJobCache.EXECUTE_JOB.add(shadowJob);
            }
        }
        if (GlobalConfig.getInstance().getNeedStopJobs() != null &&
                GlobalConfig.getInstance().getNeedStopJobs().containsKey(jobName) &&
                GlobalConfig.getInstance().getRegisteredJobs().containsKey(jobName)) {
            ShadowJob shadowJob = GlobalConfig.getInstance().getNeedRegisterJobs().get(jobName);
            boolean result = disableShaDowJob(shadowJob);
            if (result) {
                GlobalConfig.getInstance().getNeedStopJobs().remove(jobName);
                GlobalConfig.getInstance().getRegisteredJobs().remove(jobName);
                registered.remove(shadowJob.getClassName());
                ElasticJobCache.EXECUTE_JOB.remove(shadowJob);
            }
        }
        return advice.getParameterArray();

    }

    private static Field zkConfigField;

    public static boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {
        if (null != PradarSpringUtil.getBeanFactory()) {
            ZookeeperRegistryCenter registryCenter = PradarSpringUtil.getBeanFactory().getBean(ZookeeperRegistryCenter.class);
            if (null == zkConfigField) {
                zkConfigField = ZookeeperRegistryCenter.class.getDeclaredField("zkConfig");
                zkConfigField.setAccessible(true);
            }
            ZookeeperConfiguration configuration = (ZookeeperConfiguration) zkConfigField.get(registryCenter);

            String className = shaDowJob.getClassName();
            int index = className.lastIndexOf(".");
            StringBuilder serverIps = new StringBuilder(32);
            String ptClassName;
            if (ShaDowJobConstant.SIMPLE.equals(shaDowJob.getJobDataType())) {
                ptClassName = PtElasticJobSimpleJob.class.getName() + shaDowJob.getClassName();
            } else {
                ptClassName = PtDataflowJob.class.getName() + shaDowJob.getClassName();
            }
            if (-1 != index) {
                try {
                    JobOperateAPI jobOperateAPI = JobAPIFactory.createJobOperateAPI(configuration.getServerLists(), configuration.getNamespace(), configuration.getDigest());
                    Collection<String> removeList = jobOperateAPI.remove(ptClassName, null);
                    for (String serverIp : removeList) {
                        Collection<String> remove = jobOperateAPI.remove(ptClassName, serverIp);
                        if (remove.size() > 0) {
                            serverIps.append(remove.toString());
                        }
                    }
                } catch (Exception e) {
                    //ignore
                }

                if ("".equals(serverIps.toString())) {
                    registryCenter.remove("/" + ptClassName);
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
        return registered.contains(shadowJob.getClassName());
    }


    public boolean registerShadowJob(ShadowJob shadowJob) throws Throwable {
        if (null == PradarSpringUtil.getBeanFactory()) {
            logger.info("[spring-context] is null, 无法注册影子 ElasticJob");
            return false;
        }

        try {
            if (checkRegistered(shadowJob)) {
                return true;
            }
            boolean found = false;
            Map<String, JobScheduler> schedulerMap = PradarSpringUtil.getBeanFactory().getBeansOfType(JobScheduler.class);
            for (Map.Entry<String, JobScheduler> s : schedulerMap.entrySet()) {

                JobScheduler jobScheduler = s.getValue();

                LiteJobConfiguration liteJobConfiguration =
                        Reflect.on(jobScheduler).get("liteJobConfig");
                JobTypeConfiguration jobTypeConfiguration = liteJobConfiguration.getTypeConfig();
                String OriginJobName = jobTypeConfiguration.getJobClass();

                if (!OriginJobName.equals(shadowJob.getClassName())) {
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

                ElasticJob originJob = (ElasticJob) findJobInstance(jobTypeConfiguration.getJobClass());

                if (originJob == null) {
                    originJob = (ElasticJob) Class.forName(jobTypeConfiguration.getJobClass()).newInstance();
                }


                Field liteJobConfigField = JobScheduler.class.getDeclaredField("liteJobConfig");
                liteJobConfigField.setAccessible(true);
                Field regCenterField = JobScheduler.class.getDeclaredField("regCenter");
                regCenterField.setAccessible(true);
                final LiteJobConfiguration liteJobConfig = (LiteJobConfiguration) liteJobConfigField.get(jobScheduler);
                final CoordinatorRegistryCenter registryCenter = (CoordinatorRegistryCenter) regCenterField.get(jobScheduler);

                Class ptJobClass = getPtJobClass(jobTypeConfiguration.getJobType());

                LOGGER.info("shadow job registering spring className:{}, jobName:{},elasticjob.......", ptJobClass.getName(), originJob.getClass().getName());

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
                jobNameField.set(jobConfiguration.getTypeConfig().getCoreConfig(), jobConfiguration.getTypeConfig().getCoreConfig().getJobName() + originJob.getClass().getName());
                ElasticJobListener[] elasticJobListeners = null;
                if (!StringUtil.isEmpty(shadowJob.getListenerName())) {
                    Class<?> listenerClass = Class.forName(shadowJob.getListenerName());
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

                String jobBeanName = Pradar.addClusterTestPrefix(originJob.getClass().getSimpleName()) + JobScheduler.class.getSimpleName();
                // 注册bean
                defaultListableBeanFactory.registerBeanDefinition(jobBeanName, beanDefinitionBuilder.getBeanDefinition());

                JobScheduler ptSpringJobScheduler = (JobScheduler) PradarSpringUtil.getBeanFactory().getBean(jobBeanName);
                Field ptElasticJobField = ptSpringJobScheduler.getClass().getDeclaredField("elasticJob");
                ptElasticJobField.setAccessible(true);
                if (originJob instanceof SimpleJob) {
                    PtElasticJobSimpleJob ptJob = (PtElasticJobSimpleJob) ptElasticJobField.get(ptSpringJobScheduler);
                    ptJob.setSimpleJob((SimpleJob) originJob);
                } else {
                    PtDataflowJob ptJob = (PtDataflowJob) ptElasticJobField.get(ptSpringJobScheduler);
                    ptJob.setDataflowJob((DataflowJob) originJob);
                }
                if (!found) {
                    boolean ok = false;
                    try {
                        ok = processWithoutSpring(shadowJob);
                    } catch (Throwable t) {
                        logger.info("[elastic-job] register failed. jobName is {},{}"
                                , shadowJob.getClassName(), ThrowableUtils.toString(t));
                    }
                    if (ok) {
                        logger.info("[elastic-job] register success. jobName is {}", shadowJob.getClassName());
                        registered.add(shadowJob.getClassName());

                        return true;
                    }

                    logger.error("[elastic-job] 未找到相关Class信息 className is {} ", shadowJob.getClassName());
                    shadowJob.setErrorMessage("【Elastic-Job】未找到相关Class信息，error:" + shadowJob.getClassName());
                    return false;
                } else {
                    break;
                }
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return false;
        }
        registered.add(shadowJob.getClassName());

        logger.info("[elastic-job] register success. jobName is {}", shadowJob.getClassName());

        return true;
    }


    private Class getPtJobClass(JobType jobType) {
        Class ptJobClass;
        if (JobType.SIMPLE.equals(jobType)) {
            ptJobClass = PtElasticJobSimpleJob.class;
        } else {
            ptJobClass = PtDataflowJob.class;
        }
        return ptJobClass;
    }


    private Object findJobInstance(String className) {
        Map<String, ElasticJob> all =
                PradarSpringUtil.getBeanFactory().getBeansOfType(ElasticJob.class);
        for (Map.Entry<String, ElasticJob> s : all.entrySet()) {
            ElasticJob value = s.getValue();
            if (value.getClass().getName().equals(className)) {
                return value;
            }
        }
        return null;


    }


    private boolean processWithoutSpring(ShadowJob shadowJob) {
        boolean ok = false;
        Map<String, SimpleJob> map = PradarSpringUtil.getBeanFactory().getBeansOfType(SimpleJob.class);
        for (Map.Entry<String, SimpleJob> entry : map.entrySet()) {
            SimpleJob simpleJob = entry.getValue();
            if (!simpleJob.getClass().getName().equals(shadowJob.getClassName())) {
                continue;
            }
            ZookeeperRegistryCenter regCenter = getRegisterConter();
            Object originJob = simpleJob;
            ElasticSimpleJob elasticSimpleJobAnnotation = simpleJob.getClass().getAnnotation(ElasticSimpleJob.class);

            Class ptJobClass = PtElasticJobSimpleJob.class;

            String cron = StringUtils.defaultIfBlank(elasticSimpleJobAnnotation.cron(), elasticSimpleJobAnnotation.value());
            SimpleJobConfiguration simpleJobConfiguration = new SimpleJobConfiguration(JobCoreConfiguration.newBuilder(ptJobClass.getName(), cron, elasticSimpleJobAnnotation.shardingTotalCount()).shardingItemParameters(elasticSimpleJobAnnotation.shardingItemParameters()).build(), simpleJob.getClass().getCanonicalName());
            LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration.newBuilder(simpleJobConfiguration).overwrite(true).build();


            DefaultListableBeanFactory defaultListableBeanFactory = PradarSpringUtil.getBeanFactory();
            BeanDefinitionBuilder beanSimple = BeanDefinitionBuilder.rootBeanDefinition(ptJobClass);
            defaultListableBeanFactory.registerBeanDefinition(ptJobClass.getSimpleName() + originJob.getClass().getName()
                    , beanSimple.getBeanDefinition());


            ElasticJob ptJob = (ElasticJob) PradarSpringUtil.getBeanFactory()
                    .getBean(ptJobClass.getSimpleName() + originJob.getClass().getName());

            String dataSourceRef = elasticSimpleJobAnnotation.dataSource();
            if (StringUtils.isNotBlank(dataSourceRef)) {

                if (!PradarSpringUtil.getBeanFactory().containsBean(dataSourceRef)) {
                    throw new RuntimeException("not exist datasource [" + dataSourceRef + "] !");
                }

                DataSource dataSource = (DataSource) PradarSpringUtil.getBeanFactory().getBean(dataSourceRef);
                JobEventRdbConfiguration jobEventRdbConfiguration = new JobEventRdbConfiguration(dataSource);
                SpringJobScheduler jobScheduler = new SpringJobScheduler(ptJob, regCenter, liteJobConfiguration, jobEventRdbConfiguration);
                jobScheduler.init();
                if (originJob instanceof SimpleJob) {
                    ((PtElasticJobSimpleJob) Reflect.on(jobScheduler).get("elasticJob")).setSimpleJob((SimpleJob) originJob);
                }
                ok = true;
                break;
            } else {
                SpringJobScheduler jobScheduler = new SpringJobScheduler(ptJob, regCenter, liteJobConfiguration);
                jobScheduler.init();
                if (originJob instanceof SimpleJob) {
                    ((PtElasticJobSimpleJob) Reflect.on(jobScheduler).get("elasticJob")).setSimpleJob((SimpleJob) originJob);
                }
                ok = true;
                break;
            }
        }
        return ok;
    }

    private ZookeeperRegistryCenter getRegisterConter() {
        try {
            return PradarSpringUtil.getBeanFactory().getBean(ZookeeperRegistryCenter.class);
        } catch (NoSuchBeanDefinitionException t) {
            ElasticJobAutoConfiguration elasticJobAutoConfiguration =
                    PradarSpringUtil.getBeanFactory().getBean(ElasticJobAutoConfiguration.class);
            String serverList = Reflect.on(elasticJobAutoConfiguration).get("serverList");
            String namespace = Reflect.on(elasticJobAutoConfiguration).get("namespace");
            ZookeeperRegistryCenter regcenter
                    = new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
            regcenter.init();

            return regcenter;
        }
    }


}
