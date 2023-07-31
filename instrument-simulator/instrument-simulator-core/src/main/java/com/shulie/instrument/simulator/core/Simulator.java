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
package com.shulie.instrument.simulator.core;

import com.shulie.instrument.simulator.api.executors.ExecutorServiceFactory;
import com.shulie.instrument.simulator.api.guard.SimulatorGuard;
import com.shulie.instrument.simulator.api.ignore.IgnoredTypesBuilder;
import com.shulie.instrument.simulator.api.resource.ModuleLoader;
import com.shulie.instrument.simulator.core.classloader.ClassLoaderService;
import com.shulie.instrument.simulator.core.classloader.impl.DefaultClassLoaderService;
import com.shulie.instrument.simulator.core.enhance.weaver.EventListenerHandler;
import com.shulie.instrument.simulator.core.ignore.IgnoredTypesBuilderImpl;
import com.shulie.instrument.simulator.core.ignore.configurer.AdditionalLibraryIgnoredTypesConfigurer;
import com.shulie.instrument.simulator.core.ignore.configurer.GlobalIgnoredTypesConfigurer;
import com.shulie.instrument.simulator.core.ignore.configurer.InstrumentSimulatorIgnoredTypesConfigurer;
import com.shulie.instrument.simulator.core.ignore.configurer.ModulePluginIgnoredTypesConfigurer;
import com.shulie.instrument.simulator.core.manager.CoreModuleManager;
import com.shulie.instrument.simulator.core.manager.impl.*;
import com.shulie.instrument.simulator.core.util.MessageUtils;
import com.shulie.instrument.simulator.core.util.ThreadLocalCleaner;
import com.shulie.instrument.simulator.message.Messager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * 仿真器
 */
public class Simulator {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    /**
     * 需要提前加载的Simulator工具类
     */
    private final static List<String> earlyLoadSimulatorClassNameList = new ArrayList<String>();

    static {
        earlyLoadSimulatorClassNameList.add("com.shulie.instrument.simulator.core.util.SimulatorClassUtils");
        earlyLoadSimulatorClassNameList.add("com.shulie.instrument.simulator.core.util.matcher.structure.ClassStructureImplByAsm");
    }

    private CoreConfigure config;
    private CoreModuleManager coreModuleManager;
    private ClassLoaderService classLoaderService;
    private IgnoredTypesBuilder ignoredTypesBuilder;

    public Simulator(final CoreConfigure config,
                     final Instrumentation inst, ClassLoaderService classLoaderService) {
        this(config, inst, classLoaderService, false);
    }

    public Simulator(final CoreConfigure config,
                     final Instrumentation inst, ClassLoaderService classLoaderService, boolean isSyncModuleCoreManager) {
        EventListenerHandler eventListenerHandler = Messager.isInit() ? (EventListenerHandler) Messager.getEventListenerHandler() : new EventListenerHandler();
        this.config = config;
        this.ignoredTypesBuilder = buildIgnoredTypesBuilder(config);
        this.classLoaderService = classLoaderService != null ? classLoaderService : new DefaultClassLoaderService();
        this.classLoaderService.init();
        CoreModuleManager manager = initCoreModuleManager(isSyncModuleCoreManager, inst, eventListenerHandler);
        this.coreModuleManager = SimulatorGuard.getInstance().doGuard(CoreModuleManager.class, manager);

        init(eventListenerHandler);

    }

    private CoreModuleManager initCoreModuleManager(boolean isSyncModuleCoreManager, Instrumentation inst, EventListenerHandler eventListenerHandler) {
        if (isSyncModuleCoreManager) {
            return new SyncModuleCoreModuleManager(config,
                    inst,
                    new DefaultCoreLoadedClassDataSource(inst, config.isEnableUnsafe(), ignoredTypesBuilder),
                    new DefaultProviderManager(config),
                    classLoaderService,
                    eventListenerHandler,
                    new DefaultSwitcherManager(new ModuleLoader() {
                        @Override
                        public void load(Runnable runnable) {
                            runnable.run();
                        }

                        @Override
                        public void unload(Runnable runnable) {
                        }
                    }),
                    ignoredTypesBuilder);
        } else {
            return new DefaultCoreModuleManager(
                    config,
                    inst,
                    new DefaultCoreLoadedClassDataSource(inst, config.isEnableUnsafe(), ignoredTypesBuilder),
                    new DefaultProviderManager(config),
                    classLoaderService,
                    eventListenerHandler,
                    ignoredTypesBuilder,
                    new DefaultSwitcherManager(new ModuleLoader() {
                        @Override
                        public void load(Runnable runnable) {
                            runnable.run();
                        }

                        @Override
                        public void unload(Runnable runnable) {
                        }
                    })
            );
        }
    }

    private void init(EventListenerHandler eventListenerHandler) {
        doEarlyLoadSimulatorClass();
        MessageUtils.init(eventListenerHandler);
        this.coreModuleManager.onStartup();
    }

    /**
     * 提前加载某些必要的类
     */
    private void doEarlyLoadSimulatorClass() {
        for (String className : earlyLoadSimulatorClassNameList) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                //加载Simulator内部的类，不可能加载不到
            }
        }
    }

    /**
     * 获取模块管理器
     *
     * @return 模块管理器
     */
    public CoreModuleManager getCoreModuleManager() {
        return coreModuleManager;
    }

    /**
     * 销毁仿真器
     */
    public void destroy() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("SIMULATOR: prepare to destroying simulator instance. appName: {}", config.getAppName());
        }

        // uninstall all modules
        coreModuleManager.unloadAll();

        // clean MessageHandler
        MessageUtils.clean();
        // shutdown executor service factory
        ExecutorServiceFactory.getFactory().shutdown();
        /// shutdown CoreModuleManager
        this.coreModuleManager.onShutdown();
        //关闭classLoader service
        classLoaderService.dispose();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("SIMULATOR: simulator instance is destroyed success. appName: {}", config.getAppName());
        }
        SimulatorGuard.release();
        ThreadLocalCleaner.clear();
        this.config = null;
        this.coreModuleManager = null;
        this.classLoaderService = null;
    }

    public ClassLoaderService getClassLoaderService() {
        return classLoaderService;
    }

    /**
     * ignore class 配置
     */
    private IgnoredTypesBuilder buildIgnoredTypesBuilder(CoreConfigure config) {
        IgnoredTypesBuilder ignoredTypesBuilder = new IgnoredTypesBuilderImpl();
        new InstrumentSimulatorIgnoredTypesConfigurer(new DefaultSimulatorConfig(config)).configure(ignoredTypesBuilder);
        new GlobalIgnoredTypesConfigurer().configure(ignoredTypesBuilder);
        new AdditionalLibraryIgnoredTypesConfigurer().configure(ignoredTypesBuilder);
        new ModulePluginIgnoredTypesConfigurer().configure(ignoredTypesBuilder);
        return ignoredTypesBuilder;
    }
}
