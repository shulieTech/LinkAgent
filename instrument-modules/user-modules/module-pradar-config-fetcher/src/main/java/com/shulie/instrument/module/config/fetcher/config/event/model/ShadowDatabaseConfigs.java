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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceConfigModifyEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.shulie.instrument.module.config.fetcher.ConfigFetcherModule;
import com.shulie.instrument.module.config.fetcher.config.impl.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: wangjian
 * @since : 2020/9/10 16:19
 */
public class ShadowDatabaseConfigs implements IChange<Map<String, ShadowDatabaseConfig>, ApplicationConfig> {
    private final static Logger logger = LoggerFactory.getLogger(ShadowDatabaseConfigs.class.getName());

    private static ShadowDatabaseConfigs INSTANCE;

    public static ShadowDatabaseConfigs getInstance() {
        if (INSTANCE == null) {
            synchronized (ShadowDatabaseConfigs.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ShadowDatabaseConfigs();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    @Override
    public Boolean compareIsChangeAndSet(ApplicationConfig applicationConfig,
        Map<String, ShadowDatabaseConfig> newValue) {
        Set<ShadowDatabaseConfig> needCloseDataSource = new HashSet<ShadowDatabaseConfig>();
        // 同名配置比对
        for (Map.Entry<String, ShadowDatabaseConfig> old : GlobalConfig.getInstance().getShadowDatasourceConfigs()
            .entrySet()) {
            String key = old.getKey();
            ShadowDatabaseConfig newConfig = newValue.get(key);
            if (null == newConfig) {
                // 删除的配置
                needCloseDataSource.add(old.getValue());
                if (logger.isInfoEnabled()) {
                    logger.info("deleted config:" + key);
                }
                // 不需要再比对
                continue;
            }
            // 判断是否变更
            if (!newConfig.equals(old.getValue())) {
                // 修改的配置
                needCloseDataSource.add(old.getValue());
                if (logger.isInfoEnabled()) {
                    logger.info("modified config:" + key);
                }
            }

        }
        for (Map.Entry<String, ShadowDatabaseConfig> entry : newValue.entrySet()) {
            // 判断是否是新增内容
            if (null == GlobalConfig.getInstance().getShadowDatabaseConfig(entry.getKey())) {
                // 新增的配置
                needCloseDataSource.add(entry.getValue());
                if (logger.isInfoEnabled()) {
                    logger.info("added config:" + entry.getKey());
                }
            }
        }
        if (needCloseDataSource.isEmpty() && newValue.size() == GlobalConfig.getInstance().getShadowDatasourceConfigs()
            .size()) {
            return Boolean.FALSE;
        }

        GlobalConfig.getInstance().setShadowDatabaseConfigs(newValue, true);
        applicationConfig.setShadowDatabaseConfigs(GlobalConfig.getInstance().getShadowDatasourceConfigs());

        ShadowDataSourceConfigModifyEvent shadowDataSourceConfigModifyEvent = new ShadowDataSourceConfigModifyEvent(
            needCloseDataSource);
        EventRouter.router().publish(shadowDataSourceConfigModifyEvent);
        // 清除影子表模式的sql表名替换缓存
        SqlParser.clear();
        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.SHADOW_DATABASE_CONFIGS);
        if (logger.isInfoEnabled()) {
            logger.info("publish datasource config successful. new config: {}", newValue);
            for (Map.Entry<String, ShadowDatabaseConfig> entry : newValue.entrySet()) {
                logger.info(entry.getKey());
                logger.info(entry.getValue().toString());
            }
        }

        return Boolean.TRUE;
    }
}
