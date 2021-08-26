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
package com.shulie.instrument.module.register.zk.impl;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import com.pamirs.pradar.exception.PradarException;
import com.shulie.instrument.module.register.zk.ZkClient;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

public class NetflixCuratorZkClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(NetflixCuratorZkClientFactory.class);

    private static NetflixCuratorZkClientFactory INSTANCE;

    public static NetflixCuratorZkClientFactory getInstance() {
        if (INSTANCE == null) {
            synchronized (NetflixCuratorZkClientFactory.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NetflixCuratorZkClientFactory();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    private NetflixCuratorZkClientFactory() {
    }

    public ZkClient create(final ZkClientSpec spec) throws Exception {
        if (StringUtils.isBlank(spec.getZkServers())) {
            throw new PradarException("zookeeper servers is empty.");
        }
        String path = ZooKeeper.class.getProtectionDomain().getCodeSource().getLocation().toString();
        logger.info("Load ZooKeeper from {}", path);

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(spec.getZkServers())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .connectionTimeoutMs(spec.getConnectionTimeoutMillis())
                .sessionTimeoutMs(spec.getSessionTimeoutMillis())
                .threadFactory(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, spec.getThreadName());
                        t.setDaemon(true);
                        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                            @Override
                            public void uncaughtException(Thread t, Throwable e) {
                                logger.error("Thread {} caught a unknow exception with UncaughtExceptionHandler", t.getName(), e);
                            }
                        });
                        return t;
                    }
                })
                .build();
        client.start();
        logger.info("ZkClient started: {}", spec.getZkServers());

        NetflixCuratorZkClient theClient = new NetflixCuratorZkClient(client, spec.getZkServers());
        return theClient;

    }
}
