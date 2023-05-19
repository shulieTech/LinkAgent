/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.simulator.core.classloader.impl;

import com.shulie.instrument.simulator.api.ModuleRuntimeException;
import com.shulie.instrument.simulator.api.util.ObjectIdUtils;
import com.shulie.instrument.simulator.core.CoreConfigure;
import com.shulie.instrument.simulator.core.classloader.ClassLoaderFactory;
import com.shulie.instrument.simulator.core.classloader.ClassLoaderService;
import com.shulie.instrument.simulator.core.classloader.ModuleClassLoader;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/13 11:34 下午
 */
public class ClassLoaderFactoryImpl implements ClassLoaderFactory {
    private static final Logger logger = LoggerFactory.getLogger(ClassLoaderFactoryImpl.class);

    private final ClassLoaderService classLoaderService;
    private final File moduleJarFile;
    private final Set<String> importResources;
    private final String moduleId;
    private ConcurrentHashMap<Integer, ModuleClassLoader> classLoaderCache;
    private final long checksumCRC32;
    private final CoreConfigure config;
    private ModuleClassLoader defaultClassLoader;
    /**
     * 默认类加载器使用于模块的初次加载以及挑选一个对应的业务类加载器，可以允许有一个业务类加载器使用默认模块类加载器
     * 这个目的主要为了减少模块类加载器数据，保证每个模块最少一个模块类加载器即可完成模块的加载
     */
    private AtomicReference<Integer> defaultBizClassLoaderRef;
    /**
     * 是否是中间件扩展模块
     * 如果非中间件扩展模块(即为基础模块)，则默认使用默认模块类加载器加载，此模块不会因为多个业务类加载器加载而
     * 生成多个模块加载器
     */
    private final boolean isMiddlewareModule;

    public ClassLoaderFactoryImpl(final ClassLoaderService classLoaderService, final CoreConfigure config, final File moduleJarFile, final String moduleId, final boolean isMiddlewareModule, Set<String> importResources) throws IOException {
        this.classLoaderService = classLoaderService;
        this.moduleJarFile = moduleJarFile;
        this.moduleId = moduleId;
        this.config = config;
        this.isMiddlewareModule = isMiddlewareModule;
        this.classLoaderCache = new ConcurrentHashMap<Integer, ModuleClassLoader>();
        this.checksumCRC32 = FileUtils.checksumCRC32(moduleJarFile);
        this.defaultClassLoader = new ModuleClassLoader(classLoaderService, moduleJarFile, importResources, moduleId, "middleware-module-default-classloader");
        this.defaultBizClassLoaderRef = new AtomicReference<Integer>();
        this.importResources = importResources;
    }

    @Override
    public ClassLoader getDefaultClassLoader() {
        return defaultClassLoader;
    }

    @Override
    public ClassLoader getClassLoader(ClassLoader businessClassLoader) {
        if (!isMiddlewareModule) {
            return defaultClassLoader;
        }
        try {
            int id = ObjectIdUtils.identity(businessClassLoader);
            /**
             * 如果还没有默认的模块业务类加载器，则将默认模块业务类加载器设置成当前请求的业务类加载器
             * 返回默认模块类加载器
             */
            if (defaultBizClassLoaderRef.compareAndSet(null, id)) {
                return defaultClassLoader;
            }
            /**
             * 如果默认的模块业务类加载器与当前请求的业务类加载器一致，则使用默认模块类加载器
             */
            if (defaultBizClassLoaderRef.get() == id) {
                return defaultClassLoader;
            }

            ModuleClassLoader moduleClassLoader = this.classLoaderCache.get(id);
            if (moduleClassLoader != null) {
                return moduleClassLoader;
            }
            moduleClassLoader = new ModuleClassLoader(classLoaderService, moduleJarFile, importResources, moduleId, businessClassLoader == null ? null : businessClassLoader.toString());
            ModuleClassLoader oldModuleClassLoader = classLoaderCache.putIfAbsent(id, moduleClassLoader);
            if (oldModuleClassLoader != null) {
                moduleClassLoader.closeIfPossible();
                moduleClassLoader = oldModuleClassLoader;
            }
            return moduleClassLoader;
        } catch (IOException e) {
            throw new ModuleRuntimeException("SIMULATOR: getModuleClassLoader err", ModuleRuntimeException.ErrorCode.MODULE_LOAD_ERROR);
        }
    }

    @Override
    public long getChecksumCRC32() {
        return checksumCRC32;
    }

    @Override
    public void release() {
        if (classLoaderCache != null) {
            for (Map.Entry<Integer, ModuleClassLoader> entry : classLoaderCache.entrySet()) {
                entry.getValue().closeIfPossible();
            }
            classLoaderCache.clear();
            classLoaderCache = null;
        }
        if (defaultClassLoader != null) {
            defaultClassLoader.closeIfPossible();
            defaultClassLoader = null;
        }
    }
}
