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
package com.pamirs.attach.plugin.shadowjob;

import com.pamirs.attach.plugin.shadowjob.adapter.XxlJobAdapter;
import com.pamirs.attach.plugin.shadowjob.cache.ElasticJobCache;
import com.pamirs.attach.plugin.shadowjob.interceptor.*;
import com.pamirs.attach.plugin.shadowjob.util.ElasticJobRegisterUtil;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.listener.impl.ShadowImplListener;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import org.kohsuke.MetaInfServices;


/**
 * @author vincent
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "shadow-job", version = "1.0.0", author = "xiaobin@shulie.io", description = "job 支持模块，包括 quartz，elasticjob、tbschedule、lts 和 xxljob")
public class ShadowJobPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        final ShadowImplListener shaDowImplListener = new ShadowImplListener();

        try {
            XxlJobAdapter xxlJobAdapter = new XxlJobAdapter();
            GlobalConfig.getInstance().addJobAdaptor(xxlJobAdapter.getJobName(), xxlJobAdapter);
        } catch (Throwable t) {
            //ignore
        }

        EventRouter.router().addListener(shaDowImplListener);

        EventRouter.router().addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (event instanceof ClusterTestSwitchOnEvent) {

                } else if (event instanceof ClusterTestSwitchOffEvent) {
                    String message = shaDowImplListener.disableAll();
                    if ("".equals(message)) {
                        return EventResult.success("shadow-job");
                    }
                    return EventResult.error("shadow-job", message);
                }
                return EventResult.IGNORE;
            }

            @Override
            public int order() {
                return 6;
            }
        });

        enhanceTemplate.enhance(this, "org.activiti.engine.impl.cmd.AcquireAsyncJobsDueCmd", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructor = target.getConstructor("org.activiti.engine.impl.asyncexecutor.AsyncExecutor");
                constructor.addInterceptor(Listeners.of(AcquireAsyncJobsDueCmdInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "org.quartz.core.QuartzScheduler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructor = target.getConstructors();
                constructor.addInterceptor(Listeners.of(QuartzInitAdapterInterceptor.class));
            }
        });

//        enhanceTemplate.enhance(this, "com.dangdang.ddframe.job.lite.api.JobScheduler", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                InstrumentMethod constructor = target.getConstructors();
//                constructor.addInterceptor(Listeners.of(ElasticInitAdapterInterceptor.class));
//            }
//        });

        enhanceTemplate.enhance(this, "com.xxl.job.core.executor.XxlJobExecutor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod declaredMethod = target.getDeclaredMethod("start");
                declaredMethod.addInterceptor(Listeners.of(XxlInitAdapterInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "com.xxl.job.core.executor.XxlJobExecutor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod declaredMethod = target.getDeclaredMethod("loadJobThread", "int");
                declaredMethod.addInterceptor(Listeners.of(XxlOptionInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "org.springframework.context.support.ApplicationContextAwareProcessor", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("invokeAwareInterfaces", "java.lang.Object");
                getMethod.addInterceptor(Listeners.of(SpringContextInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "org.springframework.context.support.AbstractRefreshableApplicationContext", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("refreshBeanFactory");
                getMethod.addInterceptor(Listeners.of(SpringContextInterceptor.class));
            }
        });


        /**
         * com.github.ltsopensource.spring.tasktracker.JobRunnerHolder
         * lts spring任务注册切点
         */
        enhanceTemplate.enhance(this, "com.github.ltsopensource.spring.tasktracker.JobRunnerHolder",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod getMethod = target.getDeclaredMethod("add", "java.lang.String", "com.github.ltsopensource.tasktracker.runner.JobRunner");

                        getMethod.addInterceptor(Listeners.of(JobRunnerHolderAddInterceptor.class));

                    }
                });

        /**
         * com.github.ltsopensource.spring.tasktracker.JobRunnerHolder
         * lts spring任务注册切点
         */
        enhanceTemplate.enhance(this, "com.github.ltsopensource.jobtracker.support.JobReceiver",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        InstrumentMethod getMethod = target.getConstructor("com.github.ltsopensource.jobtracker.domain.JobTrackerAppContext");
                        getMethod.addInterceptor(Listeners.of(LtsJobReceiverInterceptor.class));

                    }
                });


        /**
         * com.github.ltsopensource.spring.tasktracker.JobRunnerHolder
         * lts taskTracker切点
         */

        enhanceTemplate.enhance(this, "com.github.ltsopensource.tasktracker.TaskTracker", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructor = target.getConstructors();
                constructor.addInterceptor(Listeners.of(LtsInitAdapterInterceptor.class));
            }
        });
//        enhanceTemplate.enhance(this, "org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor", new EnhanceCallback() {
//            @Override
//            public void doEnhance(InstrumentClass target) {
//                InstrumentMethod getMethod = target.getDeclaredMethod("processScheduled",
//                        "org.springframework.scheduling.annotation.Scheduled", "java.lang.reflect.Method", "java.lang.Object");
//                getMethod.addInterceptor(Listeners.of(ScheduleJobInitAdapterInterceptor.class));
//            }
//        });


        enhanceTemplate.enhance(this, "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {

                //springmvc 5.x版本
                InstrumentMethod getMethod1 = target.getDeclaredMethod("invokeHandlerMethod",
                        "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
                        "org.springframework.web.method.HandlerMethod");

                getMethod1.addInterceptor(Listeners.of(RequestMappingHandlerAdapterInterceptor.class));

                //springmvc 4.x版本
                InstrumentMethod getMethod2 = target.getDeclaredMethod("invokeHandleMethod",
                        "javax.servlet.http.HttpServletRequest", "javax.servlet.http.HttpServletResponse",
                        "org.springframework.web.method.HandlerMethod");
                getMethod2.addInterceptor(Listeners.of(RequestMappingHandlerAdapterInterceptor.class));
            }
        });

        //新增对 spring bean 的调用拦截获取 BeanFactory,通过拦截器 TransactionInterceptor 获取到 BeanFactory
        enhanceTemplate.enhance(this, "org.springframework.aop.framework.ReflectiveMethodInvocation", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod method = target.getDeclaredMethod("proceed");
                method.addInterceptor(Listeners.of(ReflectiveMethodInvocationProceedInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "com.xxl.job.core.handler.impl.MethodJobHandler", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("execute",
                        "java.lang.String");
                getMethod.addInterceptor(Listeners.of(MethodJobHandlerExecuteInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "com.dangdang.ddframe.job.executor.JobExecutorFactory", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("getJobExecutor",
                        "com.dangdang.ddframe.job.api.ElasticJob",
                        "com.dangdang.ddframe.job.executor.JobFacade");
                getMethod.addInterceptor(Listeners.of(JobExecutorFactoryGetJobExecutorInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "org.springframework.scheduling.support.ScheduledMethodRunnable", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("run");
                getMethod.addInterceptor(Listeners.of(ScheduledMethodRunnableRunInterceptor.class));
            }
        });



        enhanceTemplate.enhance(this, "org.springframework.scheduling.quartz.QuartzJobBean", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("execute", "org.quartz.JobExecutionContext");
                getMethod.addInterceptor(Listeners.of(QuartzJobBeanExecuteInterceptor.class));
            }
        });


        enhanceTemplate.enhance(this, "org.quartz.JobExecutionContext", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod constructor = target.getConstructor("org.quartz.Scheduler", "org.quartz.spi.TriggerFiredBundle", "org.quartz.Job");
                constructor.addInterceptor(Listeners.of(JobExecutionContextInterceptor.class));
            }
        });


        //org.quartz.core.JobRunShell.initialize quartz2.x
        enhanceTemplate.enhance(this, "org.quartz.core.JobRunShell", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                //2.x
                InstrumentMethod declaredMethod_1 = target.getDeclaredMethod("initialize", "org.quartz.core.QuartzScheduler");
                if (declaredMethod_1 != null) {
                    declaredMethod_1.addInterceptor(Listeners.of(JobRunShellInitializeInterceptor.class));
                }
                //1.x
                InstrumentMethod declaredMethod_2 = target.getDeclaredMethod("initialize", "org.quartz.core.QuartzScheduler", "org.quartz.spi.TriggerFiredBundle");

                if (declaredMethod_2 != null) {
                    declaredMethod_2.addInterceptor(Listeners.of(JobRunShellInitializeInterceptor_1.class));
                }

            }
        });

        enhanceTemplate.enhance(this, "org.springframework.scheduling.config.ScheduledTaskRegistrar", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod declaredMethods = target.getDeclaredMethods("scheduleTriggerTask", "scheduleCronTask",
                        "scheduleFixedRateTask", "scheduleFixedDelayTask");
                declaredMethods.addInterceptor(Listeners.of(ScheduledTaskRegistrarInterceptor.class));
            }
        });

        enhanceTemplate.enhance(this, "org.springframework.scheduling.concurrent.ReschedulingRunnable", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("run");
                getMethod.addInterceptor(Listeners.of(ReschedulingRunnableInterceptor.class));
            }
        });

        try {
            Class.forName("com.dangdang.ddframe.job.spring.schedule.SpringJobScheduler");
            PradarSpringUtil.onApplicationContextLoad(new Runnable() {
                @Override
                public void run() {
                    ElasticJobRegisterUtil.addShadowJob();
                }
            });
        } catch (ClassNotFoundException e) {
            // do nothing
        }
        return true;
    }

    @Override
    public void onUnload() throws Throwable {
        ElasticJobCache.release();
    }

}
