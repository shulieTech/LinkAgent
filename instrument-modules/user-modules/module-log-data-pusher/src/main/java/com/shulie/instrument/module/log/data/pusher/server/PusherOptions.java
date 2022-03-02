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
package com.shulie.instrument.module.log.data.pusher.server;

import com.pamirs.pradar.remoting.protocol.ProtocolCode;
import com.shulie.instrument.module.log.data.pusher.log.reader.impl.LogPusherOptions;

import java.util.List;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/11 5:48 下午
 */
public class PusherOptions {
    /**
     * zk地址
     */
    private String zkServers;
    /**
     * zk连接超时时间
     */
    private int connectionTimeoutMillis = 30000;
    /**
     * zk session timeout时间
     */
    private int sessionTimeoutMillis = 60000;

    /**
     * 服务端zk路径
     */
    private String serverZkPath;

    /**
     * 使用的data pusher
     */
    private String dataPusher;

    /**
     * 调用超时时间
     */
    private int timeout;

    /**
     * 序列化协议
     */
    private int protocolCode = ProtocolCode.JAVA;

    /**
     * http连接池大小
     */
    private int maxHttpPoolSize;

    /**
     * http请求path
     */
    private String httpPath;

    /**
     * 日志推送启动参数
     */
    private List<LogPusherOptions> logPusherOptions;

    public List<LogPusherOptions> getLogPusherOptions() {
        return logPusherOptions;
    }

    public void setLogPusherOptions(List<LogPusherOptions> logPusherOptions) {
        this.logPusherOptions = logPusherOptions;
    }

    public int getProtocolCode() {
        return protocolCode;
    }

    public void setProtocolCode(int protocolCode) {
        this.protocolCode = protocolCode;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getServerZkPath() {
        return serverZkPath;
    }

    public void setServerZkPath(String serverZkPath) {
        this.serverZkPath = serverZkPath;
    }

    public String getDataPusher() {
        return dataPusher;
    }

    public void setDataPusher(String dataPusher) {
        this.dataPusher = dataPusher;
    }

    public String getZkServers() {
        return zkServers;
    }

    public void setZkServers(String zkServers) {
        this.zkServers = zkServers;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    public int getSessionTimeoutMillis() {
        return sessionTimeoutMillis;
    }

    public void setSessionTimeoutMillis(int sessionTimeoutMillis) {
        this.sessionTimeoutMillis = sessionTimeoutMillis;
    }

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
}
