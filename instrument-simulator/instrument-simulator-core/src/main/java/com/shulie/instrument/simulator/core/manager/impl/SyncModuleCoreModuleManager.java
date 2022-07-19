package com.shulie.instrument.simulator.core.manager.impl;

import com.shulie.instrument.simulator.api.ModuleException;
import com.shulie.instrument.simulator.api.ModuleSpec;
import com.shulie.instrument.simulator.api.resource.SwitcherManager;
import com.shulie.instrument.simulator.core.CoreConfigure;
import com.shulie.instrument.simulator.core.classloader.ClassLoaderService;
import com.shulie.instrument.simulator.core.enhance.weaver.EventListenerHandler;
import com.shulie.instrument.simulator.core.manager.CoreLoadedClassDataSource;
import com.shulie.instrument.simulator.core.manager.CoreModuleManager;
import com.shulie.instrument.simulator.core.manager.ProviderManager;
import com.shulie.instrument.simulator.core.util.ModuleSpecUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Licey
 * @date 2022/5/16
 */
public class SyncModuleCoreModuleManager extends DefaultCoreModuleManager{
    /**
     * 模块模块管理
     *
     * @param config               模块核心配置
     * @param inst                 inst
     * @param classDataSource      已加载类数据源
     * @param providerManager      服务提供者管理器
     * @param classLoaderService
     * @param eventListenerHandler
     * @param switcherManager
     */
    public SyncModuleCoreModuleManager(CoreConfigure config, Instrumentation inst, CoreLoadedClassDataSource classDataSource, ProviderManager providerManager, ClassLoaderService classLoaderService, EventListenerHandler eventListenerHandler, SwitcherManager switcherManager) {
        super(config, inst, classDataSource, providerManager, classLoaderService, eventListenerHandler, switcherManager);
    }

    @Override
    public synchronized CoreModuleManager reset() throws ModuleException {
        if (isInfoEnabled) {
            logger.info("SIMULATOR-sync: resetting all loaded modules:{}", loadedModuleMap.keySet());
        }

        waitLoadModules.clear();

        // 1. 强制卸载所有模块
        unloadAll();

        // 3. 加载所有用户自定义模块, 采用异步加载方式加载用户自定义模块
        List<File> userModuleLibJars = getAllModuleLibJar(userModuleLibs);
        List<ModuleSpec> userModuleSpecs = ModuleSpecUtils.loadModuleSpecs(userModuleLibJars, false,false);
        if (!userModuleSpecs.isEmpty()) {
            initAllModuleInfos(userModuleSpecs);

            //加载
            loadModules(userModuleSpecs, "load");
        }
        if (isInfoEnabled) {
            logger.info("SIMULATOR-sync: resetting all loaded modules finished :{}", loadedModuleMap.keySet());
        }
        return this;
    }
}
