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

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import org.apache.commons.lang.StringUtils;

public class ElasticJobConfig {

    public static LiteJobConfiguration createJobConfiguration(final Class<? extends SimpleJob> jobClass,
                                                              final String cron,
                                                              final int shardingTotalCount,
                                                              final String shardingItemParameters) {
        //JobCoreConfigurationBuilder
        JobCoreConfiguration.Builder JobCoreConfigurationBuilder = JobCoreConfiguration.newBuilder(jobClass.getName(), cron, shardingTotalCount);
        //设置shardingItemParameters
        if (!StringUtils.isEmpty(shardingItemParameters)) {
            JobCoreConfigurationBuilder.shardingItemParameters(shardingItemParameters);
        }

        JobCoreConfiguration jobCoreConfiguration = JobCoreConfigurationBuilder.build();
        //创建SimpleJobConfiguration
        SimpleJobConfiguration simpleJobConfiguration = new SimpleJobConfiguration(jobCoreConfiguration, jobClass.getCanonicalName());
        //设置dump端口
        //创建LiteJobConfiguration
        LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration.newBuilder(simpleJobConfiguration).overwrite(true)
//                .monitorPort(9888)
                .build();
        return liteJobConfiguration;
    }

    /**
     * 创建支持dataFlow类型的作业的配置信息
     *
     * @param jobClass
     * @param cron
     * @param shardingTotalCount
     * @param shardingItemParameters
     * @return
     */
    public static LiteJobConfiguration createFlowJobConfiguration(final Class<? extends ElasticJob> jobClass,
                                                                  final String cron,
                                                                  final int shardingTotalCount,
                                                                  final String shardingItemParameters) {
        //JobCoreConfigurationBuilder
        JobCoreConfiguration.Builder JobCoreConfigurationBuilder = JobCoreConfiguration.newBuilder(jobClass.getName(), cron, shardingTotalCount);
        //设置shardingItemParameters
        if (!StringUtils.isEmpty(shardingItemParameters)) {
            JobCoreConfigurationBuilder.shardingItemParameters(shardingItemParameters);
        }
        JobCoreConfiguration jobCoreConfiguration = JobCoreConfigurationBuilder.build();
        // 定义数据流类型任务配置
        DataflowJobConfiguration jobConfig = new DataflowJobConfiguration(jobCoreConfiguration, jobClass.getCanonicalName(), true);
        //创建LiteJobConfiguration
        LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration.newBuilder(jobConfig).overwrite(true)
                // .monitorPort(9888)//设置dump端口
                .build();
        return liteJobConfiguration;
    }
}
