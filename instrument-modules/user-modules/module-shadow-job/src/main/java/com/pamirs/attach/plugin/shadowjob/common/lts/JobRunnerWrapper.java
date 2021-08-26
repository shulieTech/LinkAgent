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
package com.pamirs.attach.plugin.shadowjob.common.lts;

import com.github.ltsopensource.tasktracker.Result;
import com.github.ltsopensource.tasktracker.runner.JobContext;
import com.github.ltsopensource.tasktracker.runner.JobRunner;
import com.pamirs.pradar.Pradar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author angju
 * @date 2020/7/20 17:48
 */
public class JobRunnerWrapper implements JobRunner {

    private final static Logger LOGGER = LoggerFactory.getLogger(JobRunnerWrapper.class.getName());


    private boolean isShadowJob;
    private JobRunner jobRunner;
    private String shadowValue;

    public JobRunnerWrapper(String shadowValue, JobRunner jobRunner) {
        this(shadowValue, false, jobRunner);
    }

    public JobRunnerWrapper(String shadowValue, boolean isShadowJob, JobRunner jobRunner) {
        this.jobRunner = jobRunner;
        this.isShadowJob = isShadowJob;
        this.shadowValue = shadowValue;
    }

    public String getShadowValue() {
        return shadowValue;
    }

    public void setShadowJob(boolean shadowJob) {
        isShadowJob = shadowJob;
    }


    public JobRunner getJobRunner() {
        return jobRunner;
    }

    @Override
    public Result run(JobContext jobContext) throws Throwable {
        if (isShadowJob) {
            Pradar.setClusterTest(true);
        }

        try {
            return jobRunner.run(jobContext);
        } finally {
            if (isShadowJob) {
                Pradar.setClusterTest(false);
            }
        }
    }
}
