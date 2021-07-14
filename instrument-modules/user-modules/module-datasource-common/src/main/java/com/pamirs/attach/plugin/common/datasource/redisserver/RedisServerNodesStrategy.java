package com.pamirs.attach.plugin.common.datasource.redisserver;

import java.util.List;

/**
 * @Author qianfan
 * @package: com.pamirs.attach.plugin.lettuce.factory
 * @Date 2020/11/26 11:22 上午
 */
public interface RedisServerNodesStrategy {

    /**
     * 获取ip列表
     * @param obj 任意对象
     * @return ip:port:database,ip:port:database.....
     */
    List<String> match(Object obj);
}
