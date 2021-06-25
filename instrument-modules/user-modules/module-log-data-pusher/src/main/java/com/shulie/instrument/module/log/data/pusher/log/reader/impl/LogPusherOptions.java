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
package com.shulie.instrument.module.log.data.pusher.log.reader.impl;


import com.shulie.instrument.module.log.data.pusher.log.callback.LogCallback;

/**
 * @author xiaobin.zfb
 * @since 2020/8/12 3:41 下午
 */
public class LogPusherOptions {
    private String path;
    private LogCallback logCallback;
    private byte dataType;
    private int version;

    /**
     * 连续推送日志失败时的最大休眠间隔,单位毫秒
     */
    private int maxFailureSleepInterval;

    public int getMaxFailureSleepInterval() {
        return maxFailureSleepInterval;
    }

    public void setMaxFailureSleepInterval(int maxFailureSleepInterval) {
        this.maxFailureSleepInterval = maxFailureSleepInterval;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LogCallback getLogCallback() {
        return logCallback;
    }

    public void setLogCallback(LogCallback logCallback) {
        this.logCallback = logCallback;
    }

    public byte getDataType() {
        return dataType;
    }

    public void setDataType(byte dataType) {
        this.dataType = dataType;
    }
}
