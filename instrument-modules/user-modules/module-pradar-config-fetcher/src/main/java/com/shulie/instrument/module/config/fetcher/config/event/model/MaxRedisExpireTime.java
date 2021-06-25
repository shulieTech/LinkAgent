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
package com.shulie.instrument.module.config.fetcher.config.event.model;

import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.module.config.fetcher.config.impl.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @ClassName: RedisKeyAllowList
 * @author: wangjian
 * @Date: 2020/9/8 17:36
 * @Description:
 */
public class MaxRedisExpireTime implements IChange<Float, ApplicationConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaxRedisExpireTime.class.getName());
    private static final MaxRedisExpireTime INSTANCE = new MaxRedisExpireTime();

    private MaxRedisExpireTime() {
    }

    public static MaxRedisExpireTime getInstance() {
        return INSTANCE;
    }

    @Override
    public Boolean compareIsChangeAndSet(ApplicationConfig applicationConfig, Float newValue) {
        GlobalConfig.getInstance().setMaxRedisExpireTime(newValue);
        return Boolean.TRUE;
    }
}
