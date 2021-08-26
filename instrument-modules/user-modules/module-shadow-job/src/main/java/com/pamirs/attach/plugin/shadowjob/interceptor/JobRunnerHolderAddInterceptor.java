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

import com.github.ltsopensource.tasktracker.runner.JobRunner;
import com.pamirs.attach.plugin.shadowjob.adapter.LtsAdapter;
import com.pamirs.attach.plugin.shadowjob.common.lts.JobRunnerShadowRegistry;
import com.pamirs.attach.plugin.shadowjob.common.lts.JobRunnerWrapper;
import com.pamirs.attach.plugin.shadowjob.destory.JobDestroy;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author angju
 * @date 2020/7/16 20:22
 */
@Destroyable(JobDestroy.class)
public class JobRunnerHolderAddInterceptor extends ParametersWrapperInterceptorAdaptor {

    private final static Logger LOGGER = LoggerFactory.getLogger(LtsAdapter.class.getName());


    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        LOGGER.info("JobRunnerHolderAddInterceptor start");
        if (!PradarSwitcher.clusterTestSwitchOn()) {
            return args;
        }

        String shardValue = (String) args[0];
        JobRunner jobRunner = (JobRunner) args[1];
        args[1] = new JobRunnerWrapper(shardValue, JobRunnerShadowRegistry.isShadowJob(shardValue), jobRunner);
        return args;
    }
}
