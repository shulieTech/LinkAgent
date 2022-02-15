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
package com.shulie.instrument.simulator.agent.api;

import java.io.File;
import java.util.List;

import com.shulie.instrument.simulator.agent.api.model.CommandPacket;

/**
 * 外部 API，所有与配置中心交互的全部由此类完成
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/23 7:13 下午
 */
public interface ExternalAPI {

    /**
     * 下载模块包
     *
     * @param downloadPath 下载地址
     * @param targetPath   下载存放路径
     * @return
     */
    File downloadModule(String downloadPath, String targetPath);

    /**
     * 获取当前最新的命令包
     *
     * @return 返回命令包
     */
    CommandPacket getLatestCommandPacket();

    /**
     * 上报命令执行结果
     *
     * @param commandId 命令 ID
     * @param isSuccess 是否成功
     * @param errorMsg  失败信息
     */
    void reportCommandResult(long commandId, boolean isSuccess, String errorMsg);

    /**
     * 获取 agent 扫描的进程名称列表
     *
     * @return 返回 agent 扫描的所有进程列表
     */
    List<String> getAgentProcessList();
}
