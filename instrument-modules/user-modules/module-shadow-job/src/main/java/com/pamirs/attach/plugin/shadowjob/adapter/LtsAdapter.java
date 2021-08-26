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
package com.pamirs.attach.plugin.shadowjob.adapter;

import com.github.ltsopensource.tasktracker.runner.JobRunner;
import com.pamirs.attach.plugin.shadowjob.common.lts.JobRunnerShadowRegistry;
import com.pamirs.attach.plugin.shadowjob.common.lts.JobRunnerWrapper;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.adapter.JobAdapter;
import com.pamirs.pradar.internal.config.ShadowJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author angju
 * @date 2020/7/17 20:18
 */
public class LtsAdapter implements JobAdapter {
    private final static Logger LOGGER = LoggerFactory.getLogger(LtsAdapter.class.getName());


    @Override
    public String getJobName() {
        return "lts";
    }

    @Override
    public boolean registerShadowJob(ShadowJob shaDowJob) throws Throwable {
        LOGGER.info("LtsAdapter registerShaDowJob!");
        String shardValue = shaDowJob.getClassName();
        try {
            JobRunnerShadowRegistry.addShardValue(shardValue);
            Class<?> jobRunnerHolder = Class.forName("com.github.ltsopensource.spring.tasktracker.JobRunnerHolder");
            Field field = jobRunnerHolder.getDeclaredField("JOB_RUNNER_MAP");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            boolean isAccessible = modifiersField.isAccessible();
            try {
                modifiersField.setAccessible(true);
                Map<String, JobRunner> JOB_RUNNER_MAP = (Map<String, JobRunner>) field.get(null);
                JobRunner jobRunner = JOB_RUNNER_MAP.get(shardValue);
                if (jobRunner != null && (jobRunner instanceof JobRunnerWrapper)) {
                    ((JobRunnerWrapper) jobRunner).setShadowJob(false);
                    JobRunnerWrapper jobRunnerWrapper = new JobRunnerWrapper(Pradar.addClusterTestPrefixLower(shardValue), true, ((JobRunnerWrapper) jobRunner).getJobRunner());
                    JOB_RUNNER_MAP.put(Pradar.addClusterTestPrefixLower(shardValue), jobRunnerWrapper);
                } else {
                    return false;
                }
            } finally {
                field.setAccessible(isAccessible);
            }
        } catch (Exception e) {
            LOGGER.error("lts job [{}] register err", shardValue, e);
            return false;
        }

        return true;
    }

    @Override
    public boolean disableShaDowJob(ShadowJob shaDowJob) throws Throwable {

        String shardValue = shaDowJob.getClassName();
        try {
            Class<?> jobRunnerHolder = Class.forName("com.github.ltsopensource.spring.tasktracker.JobRunnerHolder");
            Field field = jobRunnerHolder.getDeclaredField("JOB_RUNNER_MAP");
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            boolean isAccessible = modifiersField.isAccessible();
            try {
                modifiersField.setAccessible(true);
                Map<String, JobRunner> JOB_RUNNER_MAP = (Map<String, JobRunner>) field.get(null);
                JobRunner jobRunner = JOB_RUNNER_MAP.get(Pradar.addClusterTestPrefixLower(shardValue));
                if (jobRunner != null && (jobRunner instanceof JobRunnerWrapper)) {
                    ((JobRunnerWrapper) jobRunner).setShadowJob(false);
                    JOB_RUNNER_MAP.remove(Pradar.addClusterTestPrefixLower(shardValue));
                }
            } finally {
                field.setAccessible(isAccessible);
            }
        } catch (Exception e) {
            LOGGER.error("lts job [{}] register err", shardValue, e);
            return false;
        }

        return true;
    }
}
