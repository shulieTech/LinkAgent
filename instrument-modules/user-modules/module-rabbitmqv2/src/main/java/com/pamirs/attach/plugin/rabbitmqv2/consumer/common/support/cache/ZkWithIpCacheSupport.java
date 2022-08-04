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
package com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.cache;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.rabbitmqv2.consumer.common.support.ConsumerApiResult;
import com.pamirs.pradar.exception.PradarException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipException;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 5:37 下午
 */
public class ZkWithIpCacheSupport extends AbstractCacheSupport implements CacheSupport {

    private volatile Map<CacheKey, ConsumerApiResult> CACHE;

    private final InterProcessLock lock;

    private final CuratorFramework zkClient;

    private final String zkDataPath;

    private final Logger logger = LoggerFactory.getLogger(ZkWithIpCacheSupport.class);

    ZkWithIpCacheSupport(CuratorFramework zkClient, String parentPath) {
        super(new WithIpCacheKeyBuilder());
        if (parentPath.endsWith("/")) {
            parentPath = parentPath.substring(0, parentPath.length() - 1);
        }
        this.zkDataPath = parentPath + "/consumers";
        this.zkClient = zkClient;
        this.lock = new InterProcessMutex(zkClient, parentPath + "/lock");
    }

    @Override
    public ConsumerApiResult computeIfAbsent(CacheKey key, Supplier supplier) {
        WithCacheKey cacheKey = (WithCacheKey)key;
        if (CACHE == null) {
            synchronized (ZkWithIpCacheSupport.class) {
                if (CACHE == null) {
                    List<ConsumerApiResult> consumerApiResults = getFromZK(cacheKey.getConnectionLocalIp());
                    if (consumerApiResults == null) {
                        renew(supplier, cacheKey.getConnectionLocalIp());
                        return CACHE.get(cacheKey);
                    } else {
                        CACHE = group(consumerApiResults);
                        ConsumerApiResult consumerApiResult = CACHE.get(cacheKey);
                        if (consumerApiResult == null) {
                            renew(supplier, cacheKey.getConnectionLocalIp());
                            return CACHE.get(cacheKey);
                        } else {
                            return consumerApiResult;
                        }
                    }
                }
            }
        }
        ConsumerApiResult consumerApiResult = CACHE.get(cacheKey);
        if (consumerApiResult == null) {
            synchronized (ZkWithIpCacheSupport.class) {
                renew(supplier, cacheKey.getConnectionLocalIp());
                consumerApiResult = CACHE.get(cacheKey);
            }
        }
        return consumerApiResult;
    }

    @Override
    public void destroy() {
        synchronized (ZkWithIpCacheSupport.class) {
            if (CACHE != null) {
                CACHE.clear();
                CACHE = null;
            }
        }
    }

    /**
     * 注意这里，不完全保证一定能拿到最新的
     */
    private void renew(Supplier supplier, String connectionLocalIp) {
        try {
            if (lock.acquire(10, TimeUnit.MILLISECONDS)) {
                try {
                    List<ConsumerApiResult> newList = supplier.get();
                    if (newList == null) {
                        throw new PradarException("supplier invoke but not data return!");
                    }
                    CACHE = group(newList);
                    putInZK(newList);
                } finally {
                    lock.release();
                }
            } else {//如果没获取到锁，锁一定是被其它节点获取，这里等到能获取到锁的时候，一定是zk上的数据已经更新了
                lock.acquire();
                try {
                    List<ConsumerApiResult> newList = getFromZK(connectionLocalIp);
                    if (newList == null) {
                        throw new PradarException("get lock but zk not update! this should never happened!");
                    }
                    CACHE = group(newList);
                } finally {
                    lock.release();
                }
            }
        } catch (Exception e) {
            throw new PradarException(e);
        }
    }

    private void putInZK(List<ConsumerApiResult> consumerApiResults) {
        try {
            if (zkClient.checkExists().forPath(zkDataPath) != null) {
                zkClient.delete().deletingChildrenIfNeeded().forPath(zkDataPath);
            }
            zkClient.create().creatingParentsIfNeeded().forPath(zkDataPath);
            Map<String, List<ConsumerApiResult>> ipGroupMap = ipGroup(consumerApiResults);
            for (Entry<String, List<ConsumerApiResult>> entry : ipGroupMap.entrySet()) {
                String ip = entry.getKey();
                String jsonStr = JSON.toJSONString(entry.getValue());
                byte[] bytes = jsonStr.getBytes("UTF-8");
                logger.info("[RabbitMQ] prepare put consumers data to zk ip {} total bytes(uncompressed) : {}", ip,
                    bytes.length);
                zkClient.create().compressed().forPath(zkIpPath(ip), bytes);
                logger.info("[RabbitMQ] put consumers data to zk ip {} total bytes(uncompressed) : {}", ip,
                    bytes.length);
            }
        } catch (Exception e) {
            throw new PradarException(e);
        }
    }

    private Map<String, List<ConsumerApiResult>> ipGroup(List<ConsumerApiResult> consumerApiResults) {
        Map<String, List<ConsumerApiResult>> result = new HashMap<String, List<ConsumerApiResult>>();
        for (ConsumerApiResult consumerApiResult : consumerApiResults) {
            String ip = ((WithCacheKey)cacheKeyBuilder.build(consumerApiResult)).getConnectionLocalIp();
            List<ConsumerApiResult> pieces = result.get(ip);
            if (pieces == null) {
                pieces = new ArrayList<ConsumerApiResult>();
                result.put(ip, pieces);
            }
            pieces.add(consumerApiResult);
        }
        return result;
    }

    private List<ConsumerApiResult> getFromZK(String connectionLocalIp) {
        try {
            if (zkClient.checkExists().forPath(zkIpPath(connectionLocalIp)) == null) {
                return null;
            }
            byte[] bytes = zkClient.getData()
                .decompressed().forPath(zkIpPath(connectionLocalIp));
            logger.info("[RabbitMQ] get consumers data from zk ip total bytes(uncompressed) : {}", bytes.length);
            String jsonStr = new String(bytes, "UTF-8");
            return JSON.parseArray(jsonStr, ConsumerApiResult.class);
        } catch (KeeperException.NoNodeException e) {
            return null;
        } catch (ZipException e) {
            return null;
        } catch (Exception e) {
            throw new PradarException(e);
        }
    }

    private String zkIpPath(String connectionLocalIp) {
        return zkDataPath + "/" + connectionLocalIp;
    }

}
