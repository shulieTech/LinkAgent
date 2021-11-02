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
import com.pamirs.pradar.internal.config.MatchConfig;
import com.pamirs.pradar.pressurement.agent.shared.exit.ArbiterHttpExit;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.module.config.fetcher.config.impl.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * @author: wangjian
 * @since : 2020/9/8 17:14
 */
public class RpcAllowList implements IChange<Set<MatchConfig>, ApplicationConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcAllowList.class.getName());
    private static RpcAllowList INSTANCE;

    public static RpcAllowList getInstance() {
        if (INSTANCE == null) {
            synchronized (RpcAllowList.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RpcAllowList();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    @Override
    public Boolean compareIsChangeAndSet(ApplicationConfig config, Set<MatchConfig> newValue) {
        config.setRpcNameWhiteList(newValue);
        GlobalConfig.getInstance().setRpcNameWhiteList(newValue);
        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.RPC_WHITE_LIST);
        ArbiterHttpExit.clearRpcMatch();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("publish rpc whitelist config successful. config={}", newValue);
        }
        // 变更后配置更新到内存
        return Boolean.TRUE;
    }
}
