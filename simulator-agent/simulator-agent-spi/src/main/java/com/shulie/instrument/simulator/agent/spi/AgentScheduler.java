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
package com.shulie.instrument.simulator.agent.spi;

import com.shulie.instrument.simulator.agent.spi.config.AgentConfig;
import com.shulie.instrument.simulator.agent.spi.config.SchedulerArgs;

/**
 * agent调度器，负责agent加载、卸载、升级等一系列操作的
 * 调度
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/16 8:53 下午
 */
public interface AgentScheduler {
    /**
     * 初始化延迟调度器
     *
     * @param schedulerArgs
     */
    void init(SchedulerArgs schedulerArgs);

    /**
     * 初始化 agent 配置
     *
     * @param agentConfig
     */
    void setAgentConfig(AgentConfig agentConfig);

    /**
     * 初始化命令执行器
     *
     * @param commandExecutor
     */
    void setCommandExecutor(CommandExecutor commandExecutor);

    /**
     * 开始调度器
     */
    void start();

    /**
     * 停止调度器
     */
    void stop();
}
