package com.pamirs.attach.plugin.common.datasource.redisserver;


import com.pamirs.pradar.internal.config.ShadowRedisConfig;

/**
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.lettuce.factory
 * @Date 2020/11/26 11:22 上午
 */
public interface RedisServerMatchStrategy {

    /**
     * 根据客户端信息返回影子库配置
     * @param obj 任意对象
     * @return ip:port:database,ip:port:database.....
     */
    ShadowRedisConfig getConfig(Object obj);
}
