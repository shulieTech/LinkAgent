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
package com.shulie.instrument.simulator.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Licey
 * @date 2021/9/24
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ListenerBehavior {
    /**
     * 是否让框架过滤压测流量。
     * <p>
     * 设置为true，只有压测流量才会进入执行 before 等方法
     */
    boolean filterClusterTest = false;

    /**
     * 是否可以过滤业务流量
     * <p>
     * 设置为true，如果不采集业务流量trace开关打开，业务流量不会进入执行 before 等方法
     */
    boolean filterBusinessData = false;

    /**
     * 是否不支持静默状态
     * <p>
     * 设置为true，就算静默开关打开了，流量也还是会进入 before 等方法
     */
    boolean noSilence = false;

    /**
     * 是否在压测未就绪时才会执行
     * <p>
     * true：压测未就绪时才会执行，false：压测就绪时才会执行
     */
    boolean executeWithClusterTestDisable = false;

    boolean isFilterClusterTest() default filterClusterTest;

    boolean isFilterBusinessData() default filterBusinessData;

    boolean isNoSilence() default noSilence;

    boolean isExecuteWithClusterTestDisable() default executeWithClusterTestDisable;
}
