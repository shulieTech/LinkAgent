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
package com.pamirs.attach.plugin.shadowjob.common;


import com.pamirs.pradar.internal.config.ShadowJob;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.shadowjob.common
 * @Date 2020-03-18 15:42
 */
public class ShaDowJobConstant {

    public static final Set<ShadowJob> registerdClass = new HashSet<ShadowJob>();

    public static final String SIMPLE = "simple";
    public static final String DATAFLOW = "dataflow";

    public static final String PLUGIN_GROUP = "PTJOB";

    public static final String SHADOW_QUARTZ = "quartz";
    public static final String SHADOW_LTS = "lts";
    public static final String SHADOW_XXL_JOB = "xxl-job";
    public static final String SHADOW_ELASTIC_JOB = "elastic-job";
    public static final String SHADOW_TB_SCHEDULE_JOB = "tbSchedule-job";
    public static final String STATUS_PLUGIN = "shadow-job";
}
