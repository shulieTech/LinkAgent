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
package com.pamirs.attach.plugin.shadowjob.common.api;

public final class JobNodePath {

    /**
     * 作业立刻触发节点名称.
     */
    public static final String TRIGGER_NODE = "trigger";

    /**
     * 作业暂停节点名称.
     */
    public static final String PAUSED_NODE = "paused";

    /**
     * 作业禁用节点名称.
     */
    public static final String DISABLED_NODE = "disabled";

    /**
     * 作业关闭节点名称.
     */
    public static final String SHUTDOWN_NODE = "shutdown";

    /**
     * 作业状态节点名称.
     */
    public static final String STATUS_NODE = "status";

    private static final String CONFIG_NODE = "config";

    private static final String LEADER_NODE = "leader";

    private static final String SERVERS_NODE = "servers";

    private static final String EXECUTION_NODE = "execution";

    private final String jobName;

    public JobNodePath(String jobName) {
        this.jobName = jobName;
    }

    /**
     * 获取节点全路径.
     *
     * @param node 节点名称
     * @return 节点全路径
     */
    public String getFullPath(final String node) {
        return String.format("/%s/%s", jobName, node);
    }

    /**
     * 获取配置节点路径.
     *
     * @param nodeName 子节点名称
     * @return 配置节点路径
     */
    public String getConfigNodePath(final String nodeName) {
        return String.format("/%s/%s/%s", jobName, CONFIG_NODE, nodeName);
    }

    /**
     * 获取作业节点IP地址根路径.
     *
     * @return 作业节点IP地址根路径
     */
    public String getServerNodePath() {
        return String.format("/%s/%s", jobName, SERVERS_NODE);
    }

    /**
     * 根据IP地址获取作业节点路径.
     *
     * @param serverIp 作业服务器IP地址
     * @return 作业节点IP地址路径
     */
    public String getServerNodePath(final String serverIp) {
        return String.format("%s/%s", getServerNodePath(), serverIp);
    }

    /**
     * 根据IP地址和子节点名称获取作业节点路径.
     *
     * @param serverIp 作业服务器IP地址
     * @param nodeName 子节点名称
     * @return 作业节点IP地址子节点路径
     */
    public String getServerNodePath(final String serverIp, final String nodeName) {
        return String.format("%s/%s", getServerNodePath(serverIp), nodeName);
    }

    /**
     * 获取运行节点根路径.
     *
     * @return 运行节点根路径
     */
    public String getExecutionNodePath() {
        return String.format("/%s/%s", jobName, EXECUTION_NODE);
    }

    /**
     * 获取运行节点路径.
     *
     * @param item     分片项
     * @param nodeName 子节点名称
     * @return 运行节点路径
     */
    public String getExecutionNodePath(final String item, final String nodeName) {
        return String.format("%s/%s/%s", getExecutionNodePath(), item, nodeName);
    }
}
