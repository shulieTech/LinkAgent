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

import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.module.config.fetcher.config.impl.ApplicationConfig;
import com.shulie.instrument.module.config.fetcher.config.utils.ObjectUtils;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/30 4:00 下午
 */
public class SearchKeyWhiteList implements IChange<Set<String>, ApplicationConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchKeyWhiteList.class.getName());
    private static SearchKeyWhiteList INSTANCE;

    public static SearchKeyWhiteList getInstance() {
        if (INSTANCE == null) {
            synchronized (SearchKeyWhiteList.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SearchKeyWhiteList();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    @Override
    public Boolean compareIsChangeAndSet(ApplicationConfig applicationConfig, Set<String> newValue) {
        Set<String> searchWhiteList = GlobalConfig.getInstance().getSearchWhiteList();
        if (ObjectUtils.equals(searchWhiteList.size(), newValue.size())
                && CollectionUtils.equals(searchWhiteList, newValue)) {
            return Boolean.FALSE;
        }
        applicationConfig.setSearchWhiteList(newValue);
        GlobalConfig.getInstance().setSearchWhiteList(newValue);
        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.SEARCH_KEY_WHITE_LIST);
        LOGGER.info("publish search key whitelist config successful. config={}", newValue);
        return Boolean.TRUE;
    }
}
