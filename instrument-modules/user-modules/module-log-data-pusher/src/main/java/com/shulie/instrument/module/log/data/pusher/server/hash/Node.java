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
package com.shulie.instrument.module.log.data.pusher.server.hash;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/8 5:02 下午
 */
public class Node {
    /**
     * 一次错误的时间间隔,当连续出错的越多,则可用的时间间隔越大
     */
    public final static long ERROR_TO_AVAILABLE_INTERVAL = 3000L;
    /**
     * 节点host
     */
    private String host;
    /**
     * 节点端口
     */
    private int port;

    /**
     * 上次节点报错的时间
     */
    private long lastErrorTime = -1;

    /**
     * 连续错误的次数
     */
    private long errorCount;


    public Node() {
    }

    public Node(String host, int port) {
        this();
        this.host = host;
        this.port = port;
    }

    public void error() {
        this.errorCount++;
        this.lastErrorTime = System.currentTimeMillis();
    }

    public long getErrorCount() {
        return this.errorCount;
    }

    public long getLastErrorTime() {
        return lastErrorTime;
    }

    public long getLastErrorTimeSec() {
        return getLastErrorTime() / 1000;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        if (port != node.port) {
            return false;
        }
        return host != null ? host.equals(node.host) : node.host == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "Node{" +
            "host='" + host + '\'' +
            ", port=" + port +
            ", lastErrorTime=" + lastErrorTime +
            ", errorCount=" + errorCount +
            '}';
    }
}
