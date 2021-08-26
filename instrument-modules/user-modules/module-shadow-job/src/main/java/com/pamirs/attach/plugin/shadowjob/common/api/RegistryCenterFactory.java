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

import java.util.concurrent.ConcurrentHashMap;

import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

/**
 * 注册中心工厂.
 *
 * @author zhangliang
 */
public final class RegistryCenterFactory {
    private static ConcurrentHashMap<String, CoordinatorRegistryCenter> registryCenterMap
        = new ConcurrentHashMap<String, CoordinatorRegistryCenter>();

    /**
     * 创建注册中心.
     *
     * @param connectString 注册中心连接字符串
     * @param namespace     注册中心命名空间
     * @param digest        注册中心凭证
     * @return 注册中心对象
     */
    public static CoordinatorRegistryCenter createCoordinatorRegistryCenter(final String connectString,
        final String namespace, final String digest) {
        String key = connectString + namespace;
        if (digest != null) {
            key += digest;
        }
        if (registryCenterMap.containsKey(key)) {
            return registryCenterMap.get(key);
        }
        ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(connectString, namespace);
        if (digest != null) {
            zkConfig.setDigest(digest);
        }
        CoordinatorRegistryCenter result = new ZookeeperRegistryCenter(zkConfig);
        result.init();
        registryCenterMap.putIfAbsent(key, result);
        return result;
    }

    public static void release() {
        if (registryCenterMap != null) {
            registryCenterMap.clear();
            registryCenterMap = null;
        }
    }
}

