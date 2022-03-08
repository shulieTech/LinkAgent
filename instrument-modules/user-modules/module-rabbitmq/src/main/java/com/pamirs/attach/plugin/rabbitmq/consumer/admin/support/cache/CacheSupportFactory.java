package com.pamirs.attach.plugin.rabbitmq.consumer.admin.support.cache;

import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2022/01/11 4:30 PM
 */
public class CacheSupportFactory {

    private volatile static CacheSupport CACHE_SUPPORT;

    private final static Logger LOGGER = LoggerFactory.getLogger(CacheSupportFactory.class);

    /**
     * 这里分为两种情况，rabbitmq.admin.api.ip.distinguish.enable=true or false
     * 1、rabbitmq控制台查询的consumer ip与客户端获取的ip一致，这种情况按缓存按ip分组（仅zk），
     * 这样在使用zk缓存时，是按ip分组储存的，效率高，且避免zk数据量过大，宽带占用
     * 2、rabbitmq控制台查询的consumer ip与客户端获取的ip不一致（一些特殊环境里面，比如获取的是slb的ip），这种情况按缓存不分组（仅zk），
     * 这样在使用zk缓存时，是按全量更新到zk上的，效率低，可能zk数据量过大，宽带占用高
     * 优先按ip分组
     */
    public static CacheSupport create(SimulatorConfig simulatorConfig) throws Exception {
        if (CACHE_SUPPORT == null) {
            synchronized (CacheSupportFactory.class) {
                if (CACHE_SUPPORT == null) {
                    CACHE_SUPPORT = build(simulatorConfig);
                }
            }
        }
        return CACHE_SUPPORT;
    }

    private static CacheSupport build(SimulatorConfig simulatorConfig) throws Exception {
        CacheSupport cacheSupport;
        if (simulatorConfig.getBooleanProperty("rabbitmq.admin.api.ip.distinguish.enable", true)) {
            if (simulatorConfig.getBooleanProperty("rabbitmq.admin.api.zk.control", false)) {
                cacheSupport = new ZkWithIpCacheSupport(ZkUtils.initZkClient(simulatorConfig),
                    simulatorConfig.getProperty("rabbitmq.admin.api.zk.control.path", "/config/log/pradar/plugin/rabbitmq"));
                LOGGER.info("[RabbitMQ] use cache support is : ZkWithIpCacheSupport");
            } else {
                cacheSupport = new SimpleLocalCacheSupport(new WithIpCacheKeyBuilder());
                LOGGER.info("[RabbitMQ] use cache support is : SimpleLocalCacheSupport WithIpCacheKeyBuilder ");
            }
        } else {
            if (simulatorConfig.getBooleanProperty("rabbitmq.admin.api.zk.control", false)) {
                cacheSupport = new ZkWithoutIpCacheSupport(ZkUtils.initZkClient(simulatorConfig),
                    simulatorConfig.getProperty("rabbitmq.admin.api.zk.control.path", "/config/log/pradar/plugin/rabbitmq"));
                LOGGER.info("[RabbitMQ] use cache support is : ZkWithoutIpCacheSupport");
            } else {
                cacheSupport = new SimpleLocalCacheSupport(new WithoutIpCacheKeyBuilder());
                LOGGER.info("[RabbitMQ] use cache support is : SimpleLocalCacheSupport WithoutIpCacheKeyBuilder ");
            }
        }
        return cacheSupport;
    }

    public static void destroy() {
        synchronized (CacheSupportFactory.class) {
            if (CACHE_SUPPORT != null) {
                CACHE_SUPPORT.destroy();
                CACHE_SUPPORT = null;
            }
        }
    }
}
