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
package com.shulie.instrument.module.log.data.pusher.server.impl;

import com.pamirs.pradar.exception.PradarException;
import com.shulie.instrument.module.log.data.pusher.server.ConnectInfo;
import com.shulie.instrument.module.log.data.pusher.server.ServerAddrProvider;
import com.shulie.instrument.module.log.data.pusher.server.ServerProviderOptions;
import com.shulie.instrument.module.log.data.pusher.server.hash.Node;
import com.shulie.instrument.module.register.zk.ZkClient;
import com.shulie.instrument.module.register.zk.ZkPathChildrenCache;
import com.shulie.instrument.module.register.zk.impl.NetflixCuratorZkClientFactory;
import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * @Description log server的服务端地址提供者,暂时未采用一致性哈希算法,主要是考虑到一致性哈希算法并不能最大化使客户端连接
 * 平均负载在每一个服务端端口上
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/8 6:05 下午
 */
public class DefaultServerAddrProvider implements ServerAddrProvider {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultServerAddrProvider.class.getName());
    private ZkClient zkClient;
    private ZkPathChildrenCache zkServerPath;
    private ServerProviderOptions serverProviderOptions;
    private List<Node> availableNodes;

    private Map<String, Object> stormServerMapping = new HashMap(16, 1);


    private int clientHash;

    public DefaultServerAddrProvider(final ServerProviderOptions serverProviderOptions) {
        if (serverProviderOptions == null) {
            throw new PradarException("ServerProviderOptions is null");
        }
        if (serverProviderOptions.getSpec() == null) {
            throw new PradarException("ZkClientSpec is null");
        }
        if (StringUtils.isBlank(serverProviderOptions.getServerZkPath())) {
            throw new PradarException("server zk path is empty");
        }

        this.serverProviderOptions = serverProviderOptions;
        /**
         * 计算当前客户端的hash值
         */
        this.clientHash = hash(UUID.randomUUID().toString());
        this.availableNodes = new ArrayList<Node>();
        try {
            this.zkClient = NetflixCuratorZkClientFactory.getInstance().create(serverProviderOptions.getSpec());
        } catch (PradarException e) {
            throw e;
        } catch (Throwable e) {
            throw new PradarException(e);
        }

        try {
            this.zkClient.ensureParentExists(serverProviderOptions.getServerZkPath());
        } catch (Throwable e) {
            LOGGER.error("ensureParentExists err:{}!", serverProviderOptions.getServerZkPath(), e);
        }
        this.zkServerPath = this.zkClient.createPathChildrenCache(serverProviderOptions.getServerZkPath());
        this.zkServerPath.setUpdateExecutor(ExecutorServiceFactory.GLOBAL_EXECUTOR_SERVICE);
        zkServerPath.setUpdateListener(new Runnable() {
            @Override
            public void run() {
                try {
                    collectLogServer();
                } catch (Throwable e) {
                    LOGGER.error("write log server path err!", e);
                }
            }
        });


        ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!zkServerPath.isRunning()) {
                        zkServerPath.startAndRefresh();
                        LOGGER.info("successfully watch log server path status from zookeeper, path={}", serverProviderOptions.getServerZkPath());
                    }
                    collectLogServer();
                } catch (Throwable e) {
                    LOGGER.error("fail to watch log server path status from zookeeper, path={}. retry next times.", serverProviderOptions.getServerZkPath(), e);
                    ExecutorServiceFactory.GLOBAL_SCHEDULE_EXECUTOR_SERVICE.schedule(this, 3, TimeUnit.SECONDS);
                }
            }
        }, 0, TimeUnit.SECONDS);
    }

    /**
     * 收集日志服务端信息
     */
    private void collectLogServer() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("SIMULATOR: start collecting log servers. current servers is {}", availableNodes);
        }

        List<String> addChildren = this.zkServerPath.getAddChildren();

        List<String> removeChildren = this.zkServerPath.getDeleteChildren();
        List<Node> addNodes = new ArrayList<Node>();
        for (String node : addChildren) {
            if (StringUtils.isBlank(node)) {
                continue;
            }
            if (StringUtils.indexOf(node, ':') == -1) {
                LOGGER.warn("listener add a valid log server,name : {}", node);
                continue;
            }
            String[] addrPort = StringUtils.split(node, ':');
            if (addrPort.length != 2) {
                LOGGER.warn("listener add a valid log server,name : {}", node);
                continue;
            }
            String addr = StringUtils.trim(addrPort[0]);
            String portStr = StringUtils.trim(addrPort[1]);
            if (!NumberUtils.isDigits(portStr)) {
                LOGGER.warn("listener add a valid log server,port is invalid,name : {}", node);
                continue;
            }
            Node n = new Node();
            n.setHost(addr);
            n.setPort(Integer.valueOf(portStr));
            addNodes.add(n);
        }

        List<Node> removeNodes = new ArrayList<Node>();
        for (String node : removeChildren) {
            if (StringUtils.isBlank(node)) {
                continue;
            }
            if (StringUtils.indexOf(node, ':') == -1) {
                LOGGER.warn("listener remove a valid log server,name : {}", node);
                continue;
            }
            String[] addrPort = StringUtils.split(node, ':');
            if (addrPort.length != 2) {
                LOGGER.warn("listener remove a valid log server,name : {}", node);
                continue;
            }
            String addr = StringUtils.trim(addrPort[0]);
            String portStr = StringUtils.trim(addrPort[1]);
            if (!NumberUtils.isDigits(portStr)) {
                LOGGER.warn("listener remove a valid log server,port is invalid,name : {}", node);
                continue;
            }
            Node n = new Node();
            n.setHost(addr);
            n.setPort(Integer.valueOf(portStr));
            removeNodes.add(n);
        }

        this.availableNodes.removeAll(removeNodes);
        this.availableNodes.addAll(addNodes);

        try {
            this.zkServerPath.refresh();
        } catch (Throwable e) {
            LOGGER.error("zkServerPath refresh error {}", e);
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("SIMULATOR: collect log servers finished. current servers is {}", availableNodes);
        }
    }


    @Override
    public ConnectInfo selectConnectInfo() {
        if (CollectionUtils.isEmpty(this.availableNodes)) {
            LOGGER.error("can't found any available log server nodes!");
            return null;
        }

        Node node = null;
        long total = Long.MAX_VALUE;
        /**
         * 取的逻辑是错误越少并且最近出错的时间越远的节点
         * 计算逻辑取出错次数 * 最近出错时间来进行计算
         */
        for (Node n : this.availableNodes) {
            /**
             * 避免值过大，错误时间只取到秒级即可
             */
            long lastErrorTime = n.getLastErrorTimeSec();
            if (n.getErrorCount() * lastErrorTime < total) {
                node = n;
                total = n.getErrorCount() * lastErrorTime;
            }
        }

        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setServerAddr(node.getHost());
        connectInfo.setPort(node.getPort());
        return connectInfo;
    }

    @Override
    public void errorConnectInfo(ConnectInfo connectInfo) {
        if (CollectionUtils.isEmpty(this.availableNodes)) {
            return;
        }
        if (connectInfo == null) {
            return;
        }
        for (Node node : this.availableNodes) {
            if (StringUtils.equals(node.getHost(), connectInfo.getServerAddr())
                    && node.getPort() == connectInfo.getPort()) {
                node.error();
                break;
            }
        }
    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
