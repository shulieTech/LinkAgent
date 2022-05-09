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
package com.pamirs.attach.plugin.shadowjob.adapter;

import com.pamirs.pradar.internal.adapter.JobAdapter;
import com.pamirs.pradar.internal.config.ShadowJob;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.shadowjob.adapter
 * @Date 2020-03-18 14:39
 */
public class XxlJobAdapter implements JobAdapter {

    public static ConcurrentMap<String, ShadowJob> needRegisterMap = new ConcurrentHashMap<String, ShadowJob>();
    public static ConcurrentMap<String, ShadowJob> registerSuccessMap = new ConcurrentHashMap<String, ShadowJob>();

    public static ConcurrentMap<String, ShadowJob> needDisableMap = new ConcurrentHashMap<String, ShadowJob>();
    public static ConcurrentMap<String, ShadowJob> disableSuccessMap = new ConcurrentHashMap<String, ShadowJob>();

    @Override
    public String getJobName() {
        return "xxl-job";
    }

    @Override
    public boolean registerShadowJob(ShadowJob shadowJob) throws Throwable {
        if (registerSuccessMap.get(shadowJob.getClassName()) != null) {
            return true;
        } else {
            needRegisterMap.put(shadowJob.getClassName(), shadowJob);
            return false;
        }
    }

    @Override
    public boolean disableShaDowJob(ShadowJob shadowJob) throws Throwable {
        if (disableSuccessMap.get(shadowJob.getClassName()) != null) {
            return true;
        } else {
            needDisableMap.put(shadowJob.getClassName(), shadowJob);
            return false;
        }
    }
}
