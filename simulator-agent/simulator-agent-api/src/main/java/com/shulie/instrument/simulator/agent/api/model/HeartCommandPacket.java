/*
 * *
 *  * Copyright 2021 Shulie Technology, Co.Ltd
 *  * Email: shulie@shulie.io
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.shulie.instrument.simulator.agent.api.model;

/**
 * @author angju
 * @date 2021/11/19 15:21
 */
class HeartCommandPacket {

    /**
     * 100110为命令id
     * 升级批次号
     */
    private static final String upgradeBath_100110_key = "upgradeBath";

    /**
     * 升级包的下载地址
     */
    private static final String downloadPath_100110_key = "downloadPath";

    /**
     * oss/ftp
     */
    private static final String pathType_100110_key = "pathType";




    /**
     * 任务编号
     */
    private String uuid;

    /**
     * 任务是否异步
     */
    private boolean sync;

    private String extrasString = null;


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isSync() {
        return sync;
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public String getExtrasString() {
        return extrasString;
    }

    public void setExtrasString(String extrasString) {
        this.extrasString = extrasString;
    }
}
