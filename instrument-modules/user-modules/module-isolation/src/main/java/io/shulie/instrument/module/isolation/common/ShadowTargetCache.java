/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shulie.instrument.module.isolation.common;

import com.pamirs.pradar.BizClassLoaderService;
import io.shulie.instrument.module.isolation.resource.ShadowResourceLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/18 13:54
 */
public class ShadowTargetCache {

    private static final Logger logger = LoggerFactory.getLogger(ShadowTargetCache.class);

    private final static Map<Object, ShadowResourceLifecycleModule> shadowTargetMap = new ConcurrentHashMap<Object, ShadowResourceLifecycleModule>();

    public static ShadowResourceLifecycleModule get(Object obj) {
        return shadowTargetMap.get(obj);
    }

    public static void put(Object key, ShadowResourceLifecycleModule value) {
        shadowTargetMap.put(key, value);
    }

    public static Map<Object, ShadowResourceLifecycleModule> getAll() {
        return shadowTargetMap;
    }

    public static void release() {
        for (Map.Entry<Object, ShadowResourceLifecycleModule> entry : shadowTargetMap.entrySet()) {
            boolean setClassLoad = false;
            try {
                ShadowResourceLifecycle resourceLifecycle = entry.getValue().getShadowResourceLifecycle();
                if (resourceLifecycle == null || resourceLifecycle.getTarget() == null) {
                    continue;
                }
                BizClassLoaderService.setBizClassLoader(resourceLifecycle.getTarget().getClass().getClassLoader());
                setClassLoad = true;
                resourceLifecycle.destroy(60);
            } catch (Throwable t) {
                logger.error("[isolation] destroy error", t);
            } finally {
                if (setClassLoad) {
                    BizClassLoaderService.clearBizClassLoader();
                }
            }
        }

        shadowTargetMap.clear();
    }


    public static ShadowResourceLifecycleModule remove(Object obj){
        return shadowTargetMap.remove(obj);
    }

}
