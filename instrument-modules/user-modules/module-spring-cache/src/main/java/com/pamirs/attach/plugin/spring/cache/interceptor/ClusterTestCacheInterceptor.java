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
package com.pamirs.attach.plugin.spring.cache.interceptor;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.cache.ClusterTestCacheWrapperKey;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/9/7 11:39 下午
 */
public class ClusterTestCacheInterceptor extends ParametersWrapperInterceptorAdaptor {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterTestCacheInterceptor.class.getName());

    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        ClusterTestUtils.validateClusterTest();
        if (!Pradar.isClusterTest()) {
            return args;
        }
        try {
            Object arg = args[0];
            /**
             * 如果已经是压测包装key对象则不处理直接返回
             *
             * 对String,byte[]和char[]特殊处理下，不再使用压测包装key包装
             */
            if (arg instanceof ClusterTestCacheWrapperKey) {
                return args;
            }

            /**
             * 如果key是String类型特殊处理下
             */
            if (arg instanceof String) {
                String key = (String) arg;
                if (!Pradar.isClusterTestPrefix(key)) {
                    key = Pradar.addClusterTestPrefix(key);
                }
                args[0] = key;
                return args;
            }

            /**
             * 如果key是byte[]类型特殊处理下
             */
            if (arg instanceof byte[]) {
                String key = new String((byte[]) arg);
                if (!Pradar.isClusterTestPrefix(key)) {
                    key = Pradar.addClusterTestPrefix(key);
                }
                args[0] = key.getBytes();
                return args;
            }

            /**
             * 如果key是char[]类型特殊处理下
             */
            if (arg instanceof char[]) {
                String key = new String((char[]) arg);
                if (!Pradar.isClusterTestPrefix(key)) {
                    key = Pradar.addClusterTestPrefix(key);
                }
                args[0] = key.toCharArray();
                return args;
            }

            ClusterTestCacheWrapperKey clusterTestCacheWrapperKey = new ClusterTestCacheWrapperKey(arg);
            args[0] = clusterTestCacheWrapperKey;
            return args;
        } catch (Throwable e) {
            LOGGER.error("[spring-cache] cluster test key wrapped err!key:{}", args, e);
            return args;
        }
    }
}
