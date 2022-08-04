package com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache;

import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.GzipCompressionProvider;
import org.apache.curator.retry.RetryForever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 3:39 PM
 */
public class ZkUtils {

    private static final Logger logger = LoggerFactory.getLogger(ZkUtils.class);

    public static CuratorFramework initZkClient(SimulatorConfig simulatorConfig) throws Exception {

        CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(simulatorConfig.getZkServers())
            .retryPolicy(new RetryForever(1000))
            .connectionTimeoutMs(simulatorConfig.getZkConnectionTimeout())
            .sessionTimeoutMs(simulatorConfig.getZkSessionTimeout())
            .compressionProvider(new GzipCompressionProvider())
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
}
