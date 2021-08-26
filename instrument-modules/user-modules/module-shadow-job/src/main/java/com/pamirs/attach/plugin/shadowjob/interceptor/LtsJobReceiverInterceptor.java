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

import com.github.ltsopensource.jobtracker.domain.JobTrackerAppContext;
import com.pamirs.attach.plugin.shadowjob.common.LtsJobTrackerAppContext;
import com.pamirs.attach.plugin.shadowjob.destory.JobDestroy;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author angju
 * @date 2020/7/19 21:40
 */
@Destroyable(JobDestroy.class)
public class LtsJobReceiverInterceptor extends TraceInterceptorAdaptor {
    @Override
    public String getPluginName() {
        return "shadow-job";
    }

    @Override
    public int getPluginType() {
        return 0;
    }

    @Override
    public void beforeFirst(Advice advice) {
        Object[] args = advice.getParameterArray();
        JobTrackerAppContext jobTrackerAppContext = (JobTrackerAppContext)args[0];
        LtsJobTrackerAppContext.setJobTrackerAppContext(jobTrackerAppContext);
    }

    @Override
    public void beforeLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        JobTrackerAppContext jobTrackerAppContext = (JobTrackerAppContext)args[0];
        LtsJobTrackerAppContext.setJobTrackerAppContext(jobTrackerAppContext);
    }

}
