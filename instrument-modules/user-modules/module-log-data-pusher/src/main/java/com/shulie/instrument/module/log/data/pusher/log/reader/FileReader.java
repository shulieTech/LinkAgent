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
package com.shulie.instrument.module.log.data.pusher.log.reader;

/**
 * @author xiaobin.zfb
 * @since 2020/8/6 7:50 下午
 */
public interface FileReader {
    /**
     * 启动,如果启动失败,则会周期性调度启动
     *
     * @return 启动是否成功
     */
    boolean start();

    /**
     * 是否已经启动
     *
     * @return 是否启动
     */
    boolean isStarted();

    /**
     * 停止
     */
    void stop();

    /**
     * 是否已经停止
     *
     * @return 是否停止
     */
    boolean isStopped();

    /**
     * 获取位点
     *
     * @return 当前位点
     */
    long getPosition();

    /**
     * 保存位点
     */
    void savePosition();

    /**
     * 获取文件路径
     *
     * @return 文件路径
     */
    String getPath();

    /**
     * 获取位点文件位置
     *
     * @return 索引文件路径
     */
    String getIdxPath();
}
