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
package com.pamirs.attach.plugin.shadowjob;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/10/19 2:50 下午
 */
public final class ShadowJobConstants {

    public final static String DYNAMIC_FIELD_JOB_RUNNER_MAP = "JOB_RUNNER_MAP";
    public final static String DYNAMIC_FIELD_SPRING_CRON_TASKS = "cronTasks";

    /**
     * spring 的org.springframework.aop.framework.ReflectiveMethodInvocation 拦截器列表名称
     */
    public final static String DYNAMIC_FIELD_INTERCEPTORS_AND_DYNAMIC_METHOD_MATCHERS = "interceptorsAndDynamicMethodMatchers";
    public final static String DYNAMIC_FIELD_BEAN_FACTORY = "beanFactory";
    public final static String DYNAMIC_FIELD_APPLICATION_CONTEXT = "applicationContext";

}
