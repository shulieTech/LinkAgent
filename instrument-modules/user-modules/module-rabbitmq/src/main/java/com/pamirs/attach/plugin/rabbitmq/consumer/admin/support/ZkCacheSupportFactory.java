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
package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support;

import java.util.concurrent.ThreadFactory;

import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/19 2:58 下午
 */
public class ZkCacheSupportFactory {

    private volatile static ZkCacheSupport ZK_CACHE_SUPPORT;

    private static final Logger logger = LoggerFactory.getLogger(ZkCacheSupportFactory.class);

    public static ZkCacheSupport create(SimulatorConfig simulatorConfig) throws Exception {
        if (ZK_CACHE_SUPPORT == null) {
            synchronized (ZkCacheSupportFactory.class) {
                if (ZK_CACHE_SUPPORT == null) {
                    ZK_CACHE_SUPPORT = new ZkCacheSupport(initZkClient(simulatorConfig),
                        simulatorConfig.getProperty("rabbitmq.admin.api.zk.control.path",
                            "/config/log/pradar/plugin/rabbitmq"));
                }
            }
        }
        return ZK_CACHE_SUPPORT;
    }

    private static CuratorFramework initZkClient(SimulatorConfig simulatorConfig) throws Exception {

        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(simulatorConfig.getZkServers())
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .connectionTimeoutMs(simulatorConfig.getZkConnectionTimeout())
            .sessionTimeoutMs(simulatorConfig.getZkSessionTimeout())
            .threadFactory(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "rabbitmq-zk-thread");
                    t.setDaemon(true);
                    t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread t, Throwable e) {
                            logger.error("Thread {} caught a unknow exception with UncaughtExceptionHandler", t.getName(),
                                e);
                        }
                    });
                    return t;
                }
            })
            .build();
        client.start();
        return client;
    }

    public static void destroy() {
        synchronized (ZkCacheSupportFactory.class) {
            ZK_CACHE_SUPPORT.destroy();
            ZK_CACHE_SUPPORT = null;
        }
    }
}
