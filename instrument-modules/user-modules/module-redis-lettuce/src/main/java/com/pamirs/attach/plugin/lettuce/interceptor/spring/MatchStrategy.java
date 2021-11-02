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
package com.pamirs.attach.plugin.lettuce.interceptor.spring;

import com.pamirs.attach.plugin.common.datasource.redisserver.RedisServerMatchStrategy;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.internal.config.ShadowRedisConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Auther: vernon
 * @Date: 2021/9/10 12:35
 * @Description:
 */
public class MatchStrategy {


    RedisServerMatchStrategy SINGLE_MODE_MATCHER = new RedisServerMatchStrategy() {
        @Override
        public ShadowRedisConfig getConfig(Object obj) {
            if (obj == null) {
                return null;
            }

            List<String> nodes = (List<String>) obj;

            if (GlobalConfig.getInstance().getShadowRedisConfigs().size() > 0 && obj != null) {

                try {
                    int count = 0;
                    for (String configKey : GlobalConfig.getInstance().getShadowRedisConfigs().keySet()) {
                        List<String> configKeys = configKey.contains(",")
                                ? Arrays.asList(StringUtils.split(configKey, ','))
                                : Arrays.asList(configKey);

                        for (String key : nodes) {
                            if (!configKeys.contains(key)) {
                                count = 0;
                                break;
                            }
                            count++;
                        }

                        if (count == nodes.size() && configKeys.size() == count) {
                            return GlobalConfig.getInstance().getShadowRedisConfig(configKey);
                        }
                    }
                } catch (Throwable e) {
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.RedisServer)
                            .setErrorCode("redisServer-0001")
                            .setMessage("获取影子数据源失败！")
                            .setDetail(ExceptionUtils.getStackTrace(e))
                            .report();
                }
            }

            throw new PressureMeasureError("not found redis shadow server config error.");

        }
    };


    RedisServerMatchStrategy CLUSTER_MODE_MATCHER = new RedisServerMatchStrategy() {
        @Override
        public ShadowRedisConfig getConfig(Object obj) {
            if (obj == null) {
                return null;
            }
            List<Key> keys = (List<Key>) obj;
            StringBuilder builder = new StringBuilder();
            for (Key k : keys) {
                builder.append(k.host.concat(":").concat(String.valueOf(k.port)))
                        .append(",");
            }

            builder.deleteCharAt(builder.length() - 1);
            List<String> nodes = new ArrayList<String>();
            nodes.add(builder.toString());

            if (GlobalConfig.getInstance().getShadowRedisConfigs().size() > 0 && obj != null) {

                try {
                    int count = 0;
                    for (String configKey : GlobalConfig.getInstance().getShadowRedisConfigs().keySet()) {
                        List<String> configKeys = configKey.contains(",")
                                ? Arrays.asList(StringUtils.split(configKey, ','))
                                : Arrays.asList(configKey);

                        for (String key : nodes) {
                            if (!configKeys.contains(key)) {
                                count = 0;
                                break;
                            }
                            count++;
                        }

                        if (count == nodes.size() && configKeys.size() == count) {
                            return GlobalConfig.getInstance().getShadowRedisConfig(configKey);
                        }
                    }
                } catch (Throwable e) {
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.RedisServer)
                            .setErrorCode("redisServer-0001")
                            .setMessage("获取影子数据源失败！")
                            .setDetail(ExceptionUtils.getStackTrace(e))
                            .report();
                }
            }

            throw new PressureMeasureError("not found redis shadow server config error.");

        }
    };
}
