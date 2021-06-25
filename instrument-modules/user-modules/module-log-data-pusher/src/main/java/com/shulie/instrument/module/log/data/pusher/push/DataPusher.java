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
package com.shulie.instrument.module.log.data.pusher.push;

import com.shulie.instrument.module.log.data.pusher.log.callback.LogCallback;
import com.shulie.instrument.module.log.data.pusher.server.ServerAddrProvider;

/**
 * 数据推送者
 *
 * @author xiaobin.zfb
 * @since 2020/8/6 10:35 下午
 */
public interface DataPusher {
    /**
     * 名称
     *
     * @return 名称
     */
    String getName();

    /**
     * 设置地址提供者
     *
     * @param provider 服务地址提供者
     */
    void setServerAddrProvider(ServerAddrProvider provider);

    /**
     * 初始化
     *
     * @param serverOptions 启动参数
     * @return 初始化是否成功
     */
    boolean init(ServerOptions serverOptions);

    /**
     * 获取日志变更时的回调函数的一个实现实例
     *
     * @return 日志处理Callback
     */
    LogCallback buildLogCallback();

    /**
     * 启动
     *
     * @return 返回启动是否成功
     */
    boolean start();

    /**
     * 停止
     */
    void stop();

}
