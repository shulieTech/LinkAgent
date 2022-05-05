/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.shulie.instrument.module.log.data.pusher.server;

/**
 * @Description http 通道配置
 * @Author ocean_wll
 * @Date 2022/3/7 4:43 下午
 */
public class HttpPushOptions {

    /**
     * http连接池大小
     */
    private int maxHttpPoolSize;

    /**
     * http请求path
     */
    private String httpPath;

    /**
     * 是否开启gzip
     */
    private boolean enableGzip;

    public int getMaxHttpPoolSize() {
        return maxHttpPoolSize;
    }

    public void setMaxHttpPoolSize(int maxHttpPoolSize) {
        this.maxHttpPoolSize = maxHttpPoolSize;
    }

    public String getHttpPath() {
        return httpPath;
    }

    public void setHttpPath(String httpPath) {
        this.httpPath = httpPath;
    }

    public boolean isEnableGzip() {
        return enableGzip;
    }

    public void setEnableGzip(boolean enableGzip) {
        this.enableGzip = enableGzip;
    }
}
