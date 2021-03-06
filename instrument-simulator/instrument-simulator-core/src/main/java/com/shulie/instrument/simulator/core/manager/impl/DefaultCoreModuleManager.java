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
package com.shulie.instrument.simulator.core.manager.impl;

import com.shulie.instrument.simulator.api.*;
import com.shulie.instrument.simulator.api.extension.ExtensionTemplate;
import com.shulie.instrument.simulator.api.guard.SimulatorGuard;
import com.shulie.instrument.simulator.api.instrument.EnhanceTemplate;
import com.shulie.instrument.simulator.api.listener.ext.BuildingForListeners;
import com.shulie.instrument.simulator.api.obj.ModuleLoadInfo;
import com.shulie.instrument.simulator.api.obj.ModuleLoadStatusEnum;
import com.shulie.instrument.simulator.api.resource.*;
import com.shulie.instrument.simulator.api.spi.DeploymentManager;
import com.shulie.instrument.simulator.core.CoreConfigure;
import com.shulie.instrument.simulator.core.CoreModule;
import com.shulie.instrument.simulator.core.classloader.ClassLoaderFactory;
import com.shulie.instrument.simulator.core.classloader.ClassLoaderService;
import com.shulie.instrument.simulator.core.classloader.impl.ClassLoaderFactoryImpl;
import com.shulie.instrument.simulator.core.enhance.weaver.EventListenerHandler;
import com.shulie.instrument.simulator.core.extension.DefaultExtensionTemplate;
import com.shulie.instrument.simulator.core.inject.ClassInjector;
import com.shulie.instrument.simulator.core.inject.impl.ModuleJarClassInjector;
import com.shulie.instrument.simulator.core.instrument.DefaultEnhanceTemplate;
import com.shulie.instrument.simulator.core.manager.ModuleLoader;
import com.shulie.instrument.simulator.core.manager.*;
import com.shulie.instrument.simulator.core.util.DefaultModuleLoadInfoManagerUtils;
import com.shulie.instrument.simulator.core.util.ModuleSpecUtils;
import com.shulie.instrument.simulator.core.util.VersionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.shulie.instrument.simulator.api.ModuleException.ErrorCode.*;
import static com.shulie.instrument.simulator.core.manager.impl.ModuleLifeCycleType.*;
import static org.apache.commons.lang.reflect.FieldUtils.writeField;

/**
 * ???????????????????????????
 */
public class DefaultCoreModuleManager implements CoreModuleManager {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final boolean isDebugEnabled = logger.isDebugEnabled();
    protected final boolean isInfoEnabled = logger.isInfoEnabled();

    protected CoreConfigure config;
    protected Instrumentation inst;
    protected ClassFileTransformer defaultClassFileTransformer;
    protected ClassInjector classInjector;
    protected CoreLoadedClassDataSource classDataSource;
    protected ProviderManager providerManager;

    /**
     * ????????????????????????
     */
    protected File[] systemModuleLibs;

    /**
     * ????????????????????????
     */
    protected File[] userModuleLibs;

    /**
     * ??????????????????
     */
    protected ClassLoaderService classLoaderService;

    /**
     * ???????????????
     */
    protected SimulatorConfig simulatorConfig;

    /**
     * ?????????????????????
     */
    protected ModuleCommandInvoker moduleCommandInvoker;

    /**
     * switcher manager
     */
    protected SwitcherManager switcherManager;

    /**
     * ?????????????????????
     */
    protected List<String> disabledModules;

    /**
     * event listener handler
     */
    protected EventListenerHandler eventListenerHandler;

    // ????????????????????????
    protected Map<String, CoreModule> loadedModuleMap = new ConcurrentHashMap<String, CoreModule>();

    /**
     * ???????????????????????????
     */
    protected Queue<Runnable> waitLoadModules = new ConcurrentLinkedQueue<Runnable>();

    /**
     * module loader
     */
    protected ModuleLoader moduleLoader;

    /**
     * ??????????????????
     *
     * @param config          ??????????????????
     * @param inst            inst
     * @param classDataSource ?????????????????????
     * @param providerManager ????????????????????????
     */
    public DefaultCoreModuleManager(final CoreConfigure config,
                                    final Instrumentation inst,
                                    final CoreLoadedClassDataSource classDataSource,
                                    final ProviderManager providerManager,
                                    final ClassLoaderService classLoaderService,
                                    final EventListenerHandler eventListenerHandler,
                                    final SwitcherManager switcherManager) {
        this.config = config;
        this.simulatorConfig = new DefaultSimulatorConfig(config);
        this.inst = inst;
        this.classDataSource = classDataSource;
        this.providerManager = providerManager;
        this.switcherManager = switcherManager;
        this.systemModuleLibs = getSystemModuleLibFiles(config.getSystemModuleLibPath());
        this.moduleLoader = new LocalModuleLoader();
        this.userModuleLibs = this.moduleLoader.loadModuleLibs(config.getAppName(), config.getUserModulePaths());
        this.classLoaderService = classLoaderService;
        this.moduleCommandInvoker = new DefaultModuleCommandInvoker(this);
        this.disabledModules = config.getDisabledModules();
        this.classInjector = new ModuleJarClassInjector(this.simulatorConfig);
        this.eventListenerHandler = eventListenerHandler;
    }

    @Override
    public void onStartup() {
        this.providerManager.onStart(simulatorConfig);
        /**
         * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? jar ????????????
         */
        this.defaultClassFileTransformer = new InternalClassFileTransformer(classInjector);
        this.inst.addTransformer(this.defaultClassFileTransformer);

        /**
         * ???????????????????????????????????????????????????
         */
        for (Map.Entry<String, List<File>> entry : simulatorConfig.getBizClassLoaderInjectFiles().entrySet()) {
            List<Class<?>> classes = classDataSource.findForReTransform(entry.getKey());
            if (CollectionUtils.isNotEmpty(classes)) {
                for (Class<?> clazz : classes) {
                    classInjector.injectClass(clazz.getClassLoader(), entry.getKey());
                }
            }
        }
    }

    @Override
    public void onShutdown() {
        this.providerManager.onShutdown(simulatorConfig);
        this.inst.removeTransformer(this.defaultClassFileTransformer);
        this.switcherManager.close();
        this.config = null;
        this.inst = null;
        this.defaultClassFileTransformer = null;
        this.classInjector.destroy();
        this.classInjector = null;
        this.classDataSource = null;
        this.providerManager = null;
        this.systemModuleLibs = null;
        this.userModuleLibs = null;
        this.classLoaderService = null;
        this.simulatorConfig = null;
        this.moduleCommandInvoker = null;
        this.disabledModules = null;
        this.eventListenerHandler = null;
        this.loadedModuleMap.clear();
        this.loadedModuleMap = null;
        this.waitLoadModules.clear();
        this.waitLoadModules = null;
        this.switcherManager = null;
    }

    /**
     * ??????????????????????????????/??????(??????)
     *
     * @param path ??????
     * @return ????????????????????????/??????(??????)
     */
    public synchronized File[] getSystemModuleLibFiles(String path) {
        if (StringUtils.isBlank(path)) {
            return new File[0];
        }
        final Collection<File> foundModuleJarFiles = new LinkedHashSet<File>();
        final File fileOfPath = new File(path);
        if (fileOfPath.isDirectory()) {
            foundModuleJarFiles.addAll(FileUtils.listFiles(new File(path), new String[]{"jar"}, true));
        } else {
            if (StringUtils.endsWithIgnoreCase(fileOfPath.getPath(), ".jar")) {
                foundModuleJarFiles.add(fileOfPath);
            }
        }

        return foundModuleJarFiles.toArray(new File[]{});
    }

    /**
     * ????????????????????????
     *
     * @param coreModule ????????????
     * @param eventType  ??????????????????????????????
     * @throws ModuleException ????????????????????????????????????????????????????????????
     */
    private void callAndFireModuleLifeCycle(final CoreModule coreModule, final int eventType) throws ModuleException {
        /**
         * ???????????????????????????????????????????????????????????????????????????
         */
        final String moduleId = coreModule.getModuleId();

        if (coreModule.getModule() instanceof ModuleLifecycle) {
            final ModuleLifecycle moduleLifecycle = (ModuleLifecycle) coreModule.getModule();
            switch (eventType) {

                case MODULE_LOAD: {
                    try {
                        moduleLifecycle.onLoad();
                    } catch (Throwable throwable) {
                        throw new ModuleException(moduleId, MODULE_LOAD_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_UNLOAD: {
                    try {
                        moduleLifecycle.onUnload();
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getModuleId(), MODULE_UNLOAD_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_ACTIVE: {
                    ModuleLoadInfo moduleLoadInfo = DefaultModuleLoadInfoManagerUtils.getDefaultModuleLoadInfoManager().
                        getModuleLoadInfos().get(moduleId);
                    if(moduleLoadInfo == null){
                        throw new ModuleException(moduleId,
                                MODULE_LOAD_ERROR,
                                String.format("moduleId : %s can not find, please check id set in @ModuleInfo and id set in module.config is same", moduleId));
                    }
                    try {

                        boolean result = moduleLifecycle.onActive();
                        if (result) {
                            moduleLoadInfo.setStatus(ModuleLoadStatusEnum.LOAD_SUCCESS);
                        } else {
                            moduleLoadInfo.setStatus(ModuleLoadStatusEnum.LOAD_DISABLE);
                        }
                        //????????????
                        if (coreModule.getModuleSpec().isSwitchAuto()) {
                            switcherManager.switchOn(coreModule.getModuleId());
                        }
                    } catch (Throwable throwable) {
                        logger.error("fail to active module :{}", moduleId, throwable);
                        moduleLoadInfo.setErrorMsg(ModuleLoadStatusEnum.LOAD_FAILED, throwable.getMessage());
                        throw new ModuleException(coreModule.getModuleId(), MODULE_ACTIVE_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_FROZEN: {
                    try {
                        moduleLifecycle.onFrozen();
                        if (coreModule.getModuleSpec().isSwitchAuto()) {
                            switcherManager.switchOff(coreModule.getModuleId());
                        }
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getModuleId(), MODULE_FROZEN_ERROR, throwable);
                    }
                    break;
                }

            }// switch
        } else {
            switch (eventType) {
                case MODULE_ACTIVE: {
                    try {
                        DefaultModuleLoadInfoManagerUtils.getDefaultModuleLoadInfoManager().
                                getModuleLoadInfos().get(moduleId).setStatus(ModuleLoadStatusEnum.LOAD_SUCCESS);
                        //????????????
                        if (coreModule.getModuleSpec().isSwitchAuto()) {
                            switcherManager.switchOn(coreModule.getModuleId());
                        }

                    } catch (Throwable throwable) {
                        DefaultModuleLoadInfoManagerUtils.getDefaultModuleLoadInfoManager().
                                getModuleLoadInfos().get(moduleId).setErrorMsg(ModuleLoadStatusEnum.LOAD_FAILED, throwable.getMessage());
                        throw new ModuleException(coreModule.getModuleId(), MODULE_ACTIVE_ERROR, throwable);
                    }
                    break;
                }

                case MODULE_FROZEN: {
                    try {
                        if (coreModule.getModuleSpec().isSwitchAuto()) {
                            switcherManager.switchOff(coreModule.getModuleId());
                        }
                    } catch (Throwable throwable) {
                        throw new ModuleException(coreModule.getModuleId(), MODULE_FROZEN_ERROR, throwable);
                    }
                    break;
                }

            }// switch
        }

        // ????????????LOAD_COMPLETED?????????????????????
        // ??????????????????????????????????????????????????????????????????????????????????????????
        if (eventType == MODULE_LOAD_COMPLETED
                && coreModule.getModule() instanceof LoadCompleted) {
            try {
                ((LoadCompleted) coreModule.getModule()).loadCompleted();
            } catch (Throwable cause) {
                logger.warn("SIMULATOR: loading module occur error when load-completed. module={};", coreModule.getModuleId(), cause);
            }
        }

    }

    /**
     * ?????????????????????
     * <p>1. ?????????????????????????????????????????????????????????</p>
     * <p>2. ?????????????????????????????????????????????</p>
     * <p>3. ?????????????????????????????????????????????</p>
     *
     * @param module             ????????????
     * @param moduleJarFile      ????????????JAR??????
     * @param classLoaderFactory ?????????????????????ClassLoader??????
     * @throws ModuleException ??????????????????
     */
    private synchronized void load(final ModuleSpec moduleSpec,
                                   final ExtensionModule module,
                                   final File moduleJarFile,
                                   final ClassLoaderFactory classLoaderFactory) throws ModuleException {

        if (loadedModuleMap.containsKey(moduleSpec.getModuleId())) {
            if (isDebugEnabled) {
                logger.debug("SIMULATOR: module already loaded. module={};", moduleSpec);
            }
            return;
        }

        if (isInfoEnabled) {
            logger.info("SIMULATOR: loading module, module={};class={};module-jar={};",
                    moduleSpec,
                    module.getClass().getName(),
                    moduleJarFile
            );
        }

        // ?????????????????????
        final CoreModule coreModule = new CoreModule(moduleSpec, moduleJarFile, classLoaderFactory, module, this);

        /**
         * ??????CoreModule???????????????
         */
        injectCoreModule(coreModule);
        // ??????@Resource??????
        injectResource(coreModule.getModule(), coreModule);

        callAndFireModuleLifeCycle(coreModule, MODULE_LOAD);

        // ?????????????????????
        coreModule.markLoaded(true);

        // ???????????????????????????????????????????????????????????????????????????????????????
        markActiveOnLoadIfNecessary(coreModule);

        // ????????????????????????
        loadedModuleMap.put(moduleSpec.getModuleId(), coreModule);

        // ???????????????????????????????????????
        callAndFireModuleLifeCycle(coreModule, MODULE_LOAD_COMPLETED);

    }

    private void injectCoreModule(final CoreModule coreModule) {
        coreModule.setCoreLoadedClassDataSource(classDataSource);
        final ModuleEventWatcher moduleEventWatcher = coreModule.append(
                new ReleaseResource<ModuleEventWatcher>(
                        SimulatorGuard.getInstance().doGuard(
                                ModuleEventWatcher.class,
                                new DefaultModuleEventWatcher(inst, classDataSource, coreModule, config.isEnableUnsafe(), eventListenerHandler)
                        )
                ) {
                    @Override
                    public void release() {
                        if (isInfoEnabled) {
                            logger.info("SIMULATOR: release all SimulatorClassFileTransformer for module={}", coreModule.getModuleId());
                        }
                        final ModuleEventWatcher moduleEventWatcher = get();
                        if (null != moduleEventWatcher) {
                            for (final SimulatorClassFileTransformer simulatorClassFileTransformer
                                    : new ArrayList<SimulatorClassFileTransformer>(coreModule.getSimulatorClassFileTransformers())) {
                                moduleEventWatcher.delete(simulatorClassFileTransformer.getWatchId());
                            }
                        }
                    }
                });
        coreModule.setModuleEventWatcher(moduleEventWatcher);
        coreModule.setModuleController(new DefaultModuleController(coreModule, this));
        coreModule.setObjectManager(new DefaultObjectManager(simulatorConfig.getInstrumentation()));
        coreModule.setModuleManager(new DefaultModuleManager(this));
        coreModule.setSimulatorConfig(simulatorConfig);
        coreModule.setEnhanceTemplate(new DefaultEnhanceTemplate(moduleEventWatcher));
        coreModule.setClassInjector(new ModuleJarClassInjector(coreModule.getSimulatorConfig()));
        coreModule.setDynamicFieldManager(new DefaultDynamicFieldManager(coreModule.getModuleId()));
        coreModule.setExtensionTemplate(new DefaultExtensionTemplate());
    }

    private static List<Field> getFieldsListWithAnnotation(final Class<?> cls, final Class<? extends Annotation> annotationCls) {
        final List<Field> allFields = new ArrayList<Field>();
        Class<?> currentClass = cls;
        while (currentClass != null) {
            final Field[] declaredFields = currentClass.getDeclaredFields();
            for (final Field field : declaredFields) {
                if (field.getAnnotation(annotationCls) != null) {
                    allFields.add(field);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return allFields;
    }

    @Override
    public void injectResource(final Object target, CoreModule coreModule) throws ModuleException {
        if (target == null) {
            return;
        }
        if (target instanceof ModuleLifecycleAdapter) {
            ((ModuleLifecycleAdapter) target).setModuleName(coreModule.getModuleId());
        }
        try {
            for (final Field resourceField : getFieldsListWithAnnotation(target.getClass(), Resource.class)) {
                final Class<?> fieldType = resourceField.getType();

                //ModuleLoadInfoManager????????????
                if (ModuleLoadInfoManager.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            DefaultModuleLoadInfoManagerUtils.getDefaultModuleLoadInfoManager(),
                            true
                    );
                }
                // LoadedClassDataSource????????????
                else if (LoadedClassDataSource.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            coreModule.getCoreLoadedClassDataSource(),
                            true
                    );
                }

                // ModuleEventWatcher????????????
                else if (ModuleEventWatcher.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            coreModule.getModuleEventWatcher(),
                            true
                    );
                }

                // ModuleController????????????
                else if (ModuleController.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            coreModule.getModuleController(),
                            true
                    );
                }

                // ModuleManager????????????
                else if (ModuleManager.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            coreModule.getModuleManager(),
                            true
                    );
                }

                // SimulatorConfig??????
                else if (SimulatorConfig.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            coreModule.getSimulatorConfig(),
                            true
                    );
                }
                //EnhanceTemplate??????
                else if (EnhanceTemplate.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            coreModule.getEnhanceTemplate(),
                            true
                    );
                }
                // ModuleCommandInvoker ??????
                else if (ModuleCommandInvoker.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, moduleCommandInvoker, true);
                }
                // ObjectManager ??????
                else if (ObjectManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, coreModule.getObjectManager(), true);
                }
                // DynamicFieldManager ??????
                else if (DynamicFieldManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, coreModule.getDynamicFieldManager(), true);
                }
                // inject switcher manager
                else if (SwitcherManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, switcherManager, true);
                }
                else if (ExtensionTemplate.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, coreModule.getExtensionTemplate(), true);
                }
                // ????????????????????????????????????
                else {
                    logger.warn("SIMULATOR: module inject @Resource ignored: field not found. module={};class={};type={};field={};",
                            coreModule.getModuleId(),
                            coreModule.getModule().getClass().getName(),
                            fieldType.getName(),
                            resourceField.getName()
                    );
                }

            }
        } catch (IllegalAccessException cause) {
            throw new ModuleException(coreModule.getModuleId(), MODULE_LOAD_ERROR, cause);
        }
    }

    public void deInjectResource(final Object target, CoreModule coreModule) throws ModuleException {
        if (target == null) {
            return;
        }
        if (target instanceof ModuleLifecycleAdapter) {
            ((ModuleLifecycleAdapter) target).setModuleName(coreModule.getModuleId());
        }
        try {
            for (final Field resourceField : getFieldsListWithAnnotation(target.getClass(), Resource.class)) {
                final Class<?> fieldType = resourceField.getType();

                // LoadedClassDataSource????????????
                if (LoadedClassDataSource.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            null,
                            true
                    );
                }

                // ModuleEventWatcher????????????
                else if (ModuleEventWatcher.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            null,
                            true
                    );
                }

                // ModuleController????????????
                else if (ModuleController.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            null,
                            true
                    );
                }

                // ModuleManager????????????
                else if (ModuleManager.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            null,
                            true
                    );
                }

                // SimulatorConfig??????
                else if (SimulatorConfig.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            null,
                            true
                    );
                }
                //EnhanceTemplate??????
                else if (EnhanceTemplate.class.isAssignableFrom(fieldType)) {
                    writeField(
                            resourceField,
                            target,
                            null,
                            true
                    );
                }
                // ModuleCommandInvoker ??????
                else if (ModuleCommandInvoker.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, null, true);
                }
                // ObjectManager ??????
                else if (ObjectManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, null, true);
                }
                // DynamicFieldManager ??????
                else if (DynamicFieldManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, null, true);
                }
                // DeploymentManager inject
                else if (DeploymentManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, null, true);
                }
                // inject switcher manager
                else if (SwitcherManager.class.isAssignableFrom(fieldType)) {
                    writeField(resourceField, target, null, true);
                }

            }
        } catch (IllegalAccessException cause) {
            throw new ModuleException(coreModule.getModuleId(), MODULE_LOAD_ERROR, cause);
        }
    }

    private void markActiveOnLoadIfNecessary(final CoreModule coreModule) throws ModuleException {
        if (isInfoEnabled) {
            logger.info("SIMULATOR: active module when OnLoad, module={}", coreModule.getModuleId());
        }
        if (coreModule.isActiveOnLoad()) {
            active(coreModule);
        }
    }

    /**
     * ???????????????????????????
     * <p>1. ??????????????????????????????????????????????????????</p>
     * <p>2. ???????????????????????????????????????</p>
     * <p>3. ??????????????????????????????????????????</p>
     *
     * @param coreModule              ????????????????????????
     * @param isIgnoreModuleException ????????????????????????
     * @throws ModuleException ??????????????????
     */
    @Override
    public synchronized CoreModule unload(final CoreModule coreModule,
                                          final boolean isIgnoreModuleException) throws ModuleException {

        return unload(coreModule, isIgnoreModuleException, true);
    }

    /**
     * ???????????????????????????
     * <p>1. ??????????????????????????????????????????????????????</p>
     * <p>2. ???????????????????????????????????????</p>
     * <p>3. ??????????????????????????????????????????</p>
     *
     * @param coreModule              ????????????????????????
     * @param isIgnoreModuleException ????????????????????????
     * @param frozen                  ??????????????????????????????
     * @throws ModuleException ??????????????????
     */
    @Override
    public CoreModule unload(CoreModule coreModule, boolean isIgnoreModuleException, boolean frozen) throws ModuleException {
        if (!coreModule.isLoaded()) {
            if (isDebugEnabled) {
                logger.debug("SIMULATOR: module already unLoaded. module={};", coreModule.getModuleId());
            }
            return coreModule;
        }

        if (isInfoEnabled) {
            logger.info("SIMULATOR: unloading module, module={};class={};",
                    coreModule.getModuleId(),
                    coreModule.getModule().getClass().getName()
            );
        }

        if (frozen) {
            // ??????????????????
            frozen(coreModule, isIgnoreModuleException);
        }

        // ??????????????????
        try {
            callAndFireModuleLifeCycle(coreModule, MODULE_UNLOAD);
        } catch (ModuleException meCause) {
            if (isIgnoreModuleException) {
                logger.warn("SIMULATOR: unload module occur error, ignored. module={};class={};code={};",
                        meCause.getModuleId(),
                        coreModule.getModule().getClass().getName(),
                        meCause.getErrorCode(),
                        meCause
                );
            } else {
                throw meCause;
            }
        }

        String moduleId = coreModule.getModuleId();
        ClassLoaderFactory classLoaderFactory = coreModule.getClassLoaderFactory();
        // ???????????????????????????
        loadedModuleMap.remove(moduleId);
        // ???????????????????????????
        coreModule.markLoaded(false);
        // de inject resource, free class easy
        deInjectResource(coreModule.getModule(), coreModule);
        // can't get moduleId & classLoaderFactory from module after releaseAll
        // so get moduleId & classLoaderFactory before releaseAll invoke
        coreModule.releaseAll();

        //???????????????????????????
        classLoaderService.unload(moduleId, classLoaderFactory);

        // ????????????ClassLoader
        closeModuleJarClassLoaderIfNecessary(classLoaderFactory);

        return coreModule;
    }

    @Override
    public void unloadAll() {
        if (isInfoEnabled) {
            logger.info("SIMULATOR: force unloading all loaded modules:{}", loadedModuleMap.keySet());
        }
        //???????????????????????????????????????
        List<CoreModule> modules = new ArrayList<CoreModule>(loadedModuleMap.values());
        List<CoreModule> userModules = new ArrayList<CoreModule>();
        List<CoreModule> systemModules = new ArrayList<CoreModule>();
        for (CoreModule coreModule : modules) {
            if (coreModule.getModuleSpec().isSystemModule()) {
                systemModules.add(coreModule);
            } else {
                userModules.add(coreModule);
            }
        }

        // uninstall user modules first
        uninstall(userModules);
        uninstall(systemModules);
    }

    /**
     * unload module in order
     *
     * @param coreModule
     * @param modules
     */
    private void unloadInOrder(CoreModule coreModule, List<CoreModule> modules) {
        if (!coreModule.isLoaded()) {
            return;
        }
        Iterator<CoreModule> it = modules.iterator();
        while (it.hasNext()) {
            CoreModule module = it.next();
            // ignore if moduleSpec is null or dependencies is null
            if (!module.isLoaded() || !module.getModuleSpec().getDependencies().contains(coreModule.getModuleId())) {
                continue;
            }
            unloadInOrder(module, modules);
        }
        try {
            unload(coreModule, true, false);
        } catch (ModuleException cause) {
            // ?????????????????????????????????????????????????????????????????????
            logger.warn("SIMULATOR: force unloading module occur error! module={};", coreModule.getModuleId(), cause);
        }
    }

    private void uninstall(List<CoreModule> modules) {
        for (CoreModule coreModule : modules) {
            if (!coreModule.getModuleSpec().isMiddlewareModule()) {
                try {
                    frozen(coreModule, true);
                } catch (ModuleException cause) {
                    // ?????????????????????????????????????????????????????????????????????
                    logger.warn("SIMULATOR: force frozen module occur error! module={};", coreModule.getModuleId(), cause);
                }
            }
        }

        for (CoreModule coreModule : modules) {
            if (coreModule.getModuleSpec().isMiddlewareModule()) {
                try {
                    frozen(coreModule, true);
                } catch (ModuleException cause) {
                    // ?????????????????????????????????????????????????????????????????????
                    logger.warn("SIMULATOR: force frozen module occur error! module={};", coreModule.getModuleId(), cause);
                }
            }
        }

        for (CoreModule coreModule : modules) {
            unloadInOrder(coreModule, modules);
        }
    }

    @Override
    public ClassLoaderService getClassLoaderService() {
        return classLoaderService;
    }

    @Override
    public synchronized void active(final CoreModule coreModule) throws ModuleException {

        // ???????????????????????????????????????????????????
        if (coreModule.isActivated()) {
            if (isDebugEnabled) {
                logger.debug("SIMULATOR: module already activated. module={};", coreModule.getModuleId());
            }
            return;
        }
        if (isInfoEnabled) {
            logger.info("SIMULATOR: active module, module={};class={};module-jar={};",
                    coreModule.getModuleId(),
                    coreModule.getModule().getClass().getName(),
                    coreModule.getJarFile()
            );
        }

        // ??????????????????
        callAndFireModuleLifeCycle(coreModule, MODULE_ACTIVE);

        // ?????????????????????
        for (final SimulatorClassFileTransformer simulatorClassFileTransformer : coreModule.getSimulatorClassFileTransformers()) {
            List<BuildingForListeners> list = simulatorClassFileTransformer.getAllListeners();
            if (CollectionUtils.isNotEmpty(list)) {
                for (BuildingForListeners buildingForListeners : list) {
                    eventListenerHandler.active(
                            buildingForListeners.getListenerId(),
                            simulatorClassFileTransformer.getEventListeners().get(buildingForListeners.getListenerId()),
                            buildingForListeners.getEventTypes()
                    );
                }
            }


        }

        // ???????????????????????????
        coreModule.markActivated(true);
    }

    @Override
    public synchronized void frozen(final CoreModule coreModule,
                                    final boolean isIgnoreModuleException) throws ModuleException {

        // ???????????????????????????(???????????????)????????????????????????
        if (!coreModule.isActivated()) {
            if (isDebugEnabled) {
                logger.debug("SIMULATOR: module already frozen. module={};", coreModule.getModuleId());
            }
            return;
        }

        if (isInfoEnabled) {
            logger.info("SIMULATOR: frozen module, module={};class={};module-jar={};",
                    coreModule.getModuleId(),
                    coreModule.getModule().getClass().getName(),
                    coreModule.getJarFile()
            );
        }

        // ??????????????????
        try {
            callAndFireModuleLifeCycle(coreModule, MODULE_FROZEN);
        } catch (ModuleException meCause) {
            if (isIgnoreModuleException) {
                logger.warn("SIMULATOR: frozen module occur error, ignored. module={};class={};code={};",
                        meCause.getModuleId(),
                        coreModule.getModule().getClass().getName(),
                        meCause.getErrorCode(),
                        meCause
                );
            } else {
                throw meCause;
            }
        }

        // ?????????????????????
        for (final SimulatorClassFileTransformer simulatorClassFileTransformer : coreModule.getSimulatorClassFileTransformers()) {
            for (BuildingForListeners buildingForListeners : simulatorClassFileTransformer.getAllListeners()) {
                eventListenerHandler.frozen(buildingForListeners.getListenerId());
            }

        }

        // ???????????????????????????
        coreModule.markActivated(false);
    }

    @Override
    public Collection<CoreModule> list() {
        return loadedModuleMap.values();
    }

    @Override
    public CoreModule get(String moduleId) {
        return loadedModuleMap.get(moduleId);
    }

    @Override
    public CoreModule getThrowsExceptionIfNull(String moduleId) throws ModuleException {
        final CoreModule coreModule = get(moduleId);
        if (null == coreModule) {
            throw new ModuleException(moduleId, MODULE_NOT_EXISTED);
        }
        return coreModule;
    }


    private boolean isOptimisticDirectoryContainsFile(final File directory,
                                                      final File child) {
        try {
            return directoryContains(directory, child);
        } catch (IOException cause) {
            // ???????????????????????????????????????directory??????child????????????
            // ????????????TRUE????????????????????????????????????????????????????????????
            // ?????????????????????,?????????????????????USER?????????????????????IOException?????????
            logger.warn("SIMULATOR: occur OptimisticDirectoryContainsFile: directory={} or child={} maybe broken.", directory, child, cause);
            return true;
        }
    }

    public static boolean directoryContains(final File directory, final File child) throws IOException {

        // Fail fast against NullPointerException
        if (directory == null) {
            throw new IllegalArgumentException("Directory must not be null");
        }

        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        if (child == null) {
            return false;
        }

        if (!directory.exists() || !child.exists()) {
            return false;
        }

        // Canonicalize paths (normalizes relative paths)
        final String canonicalParent = directory.getCanonicalPath();
        final String canonicalChild = child.getCanonicalPath();

        return directoryContains(canonicalParent, canonicalChild);
    }

    public static boolean directoryContains(final String canonicalParent, final String canonicalChild)
            throws IOException {

        // Fail fast against NullPointerException
        if (canonicalParent == null) {
            throw new IllegalArgumentException("Directory must not be null");
        }

        if (canonicalChild == null) {
            return false;
        }

        if (IOCase.SYSTEM.checkEquals(canonicalParent, canonicalChild)) {
            return false;
        }

        return IOCase.SYSTEM.checkStartsWith(canonicalChild, canonicalParent);
    }

    private boolean isSystemModule(final File child) {
        return isOptimisticDirectoryContainsFile(new File(config.getSystemModuleLibPath()), child);
    }

    /**
     * ??????????????????????????????
     */
    final private class InnerModuleJarLoadCallback implements ModuleLibLoader.ModuleJarLoadCallback {
        @Override
        public void onLoad(final SimulatorConfig simulatorConfig, final ModuleSpec moduleSpec, final File moduleJarFile) throws Throwable {
            providerManager.loading(simulatorConfig, moduleSpec, moduleJarFile);
        }
    }

    /**
     * ????????????????????????
     */
    final private class InnerModuleLoadCallback implements ModuleJarLoader.ModuleLoadCallback {
        @Override
        public void onLoad(final SimulatorConfig simulatorConfig,
                           final ModuleSpec moduleSpec,
                           final Class moduleClass,
                           final ExtensionModule module,
                           final File moduleJarFile,
                           final ClassLoaderFactory classLoaderFactory) throws Throwable {

            // ????????????????????????????????????ID??????????????????????????????????????????
            if (loadedModuleMap.containsKey(moduleSpec.getModuleId())) {
                final CoreModule existedCoreModule = get(moduleSpec.getModuleId());
                if (isInfoEnabled) {
                    logger.info("SIMULATOR: module already loaded, ignore load this module. expected:module={};class={};loader={}|existed:class={};loader={};",
                            moduleSpec.getModuleId(),
                            moduleClass, classLoaderFactory,
                            existedCoreModule.getModule().getClass().getName(),
                            existedCoreModule.getClassLoaderFactory()
                    );
                }
                return;
            }

            // ????????????ModuleLoadingChain?????????
            providerManager.loading(
                    simulatorConfig,
                    moduleClass,
                    module,
                    moduleJarFile,
                    classLoaderFactory.getDefaultClassLoader()
            );

            // ??????????????????????????????????????????
            if (isInfoEnabled) {
                logger.info("SIMULATOR: found new module, prepare to load. module={};class={};loader={};",
                        moduleSpec.getModuleId(),
                        moduleClass,
                        classLoaderFactory
                );
            }

            // ?????????????????????????????????
            load(moduleSpec, module, moduleJarFile, classLoaderFactory);
        }
    }

    @Override
    public synchronized void flush(final boolean isForce) throws ModuleException {
        if (isForce) {
            forceFlush();
        } else {
            softFlush();
        }
    }

    protected List<File> getAllModuleLibJar(File[] libDirs) {
        List<File> files = new ArrayList<File>();
        for (File file : libDirs) {
            loadJar(file, files);
        }
        return files;
    }

    private void loadJar(File file, List<File> files) {
        if (!file.exists() || !file.canRead()) {
            return;
        }
        if (file.isFile()) {
            if (file.getName().endsWith(".jar")) {
                files.add(file);
            }
            return;
        }
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            for (File f : subFiles) {
                loadJar(f, files);
            }
        }
    }

    private void loadModule(final ModuleSpec moduleSpec) {
        /**
         * ????????????????????????
         */
        if (moduleSpec.isLoaded()) {
            return;
        }
        // ?????????????????????????????????????????????????????????????????????
        // ?????????????????????????????????
        if (moduleSpec.getFile().exists() && moduleSpec.getFile().canRead()) {
            new ModuleLibLoader(moduleSpec, config.getLaunchMode(), classLoaderService)
                    .load(simulatorConfig,
                            new InnerModuleJarLoadCallback(),
                            new InnerModuleLoadCallback()
                    );
        } else {
            logger.warn("SIMULATOR: module-lib not access, ignore flush load this lib. path={}, module-id={}", moduleSpec.getFile(), moduleSpec.getModuleId());
        }
    }

    /**
     * ????????????
     *
     * @param moduleSpec
     * @param action
     */
    private void loadModule(final ModuleSpec moduleSpec, String action) {
        if (isInfoEnabled) {
            logger.info("SIMULATOR: prepare to load module {} ,file={}", moduleSpec.getModuleId(), moduleSpec.getFile().getAbsolutePath());
        }
        if (!moduleSpec.getFile().exists() || !moduleSpec.getFile().canRead()) {
            moduleSpec.setValid(false);
            logger.warn("SIMULATOR: {} modules[{}]: module-lib can not access, cause by file is not exists or can't read. module-lib={}, exists={}, canRead={}",
                    action, moduleSpec.getModuleId(), moduleSpec.getFile(), moduleSpec.getFile().exists(), moduleSpec.getFile().canRead());
            return;
        }

        /**
         * ??????????????????????????????????????????????????? Module
         */
        if (!(VersionUtils.isLeVersion(moduleSpec.getSinceVersion(), simulatorConfig.getSimulatorVersion()) && VersionUtils.isGeVersion(moduleSpec.getUntilVersion(), simulatorConfig.getSimulatorVersion()))) {
            moduleSpec.setValid(false);
            logger.warn("SIMULATOR: {} modules[{}]: module is not enabled, cause by module version is not support simulator version, will be ignored. module-lib={}, simulator-version:{} module-support-version:{}-{}",
                    action, moduleSpec.getModuleId(), moduleSpec.getFile(), simulatorConfig.getSimulatorVersion(), moduleSpec.getSinceVersion(), moduleSpec.getUntilVersion());
            return;
        }

        /**
         * ?????????????????????????????????????????????????????????????????????
         */
        if (!moduleSpec.isMustUse()) {
            if (disabledModules.contains(moduleSpec.getModuleId())) {
                moduleSpec.setValid(false);
                logger.warn("SIMULATOR: {} modules[{}]: module is disabled, will be ignored. module-lib={}", action, moduleSpec.getModuleId(), moduleSpec.getFile());
                return;
            }
        }

        try {
            final ClassLoaderFactory moduleClassLoader = new ClassLoaderFactoryImpl(classLoaderService, config, moduleSpec.getFile(), moduleSpec.getModuleId(), moduleSpec.isMiddlewareModule());
            classLoaderService.load(moduleSpec, moduleClassLoader);
        } catch (Throwable e) {
            logger.info("load module [{}] fail, set module invalid", moduleSpec.getModuleId(), e);
            moduleSpec.setValid(false);
        }
        if (isInfoEnabled) {
            logger.info("SIMULATOR: {} modules[{}]: load module success. module-lib={}", action, moduleSpec.getModuleId(), moduleSpec.getFile());
        }
        if (CollectionUtils.isNotEmpty(moduleSpec.getDependencies())) {
            /**
             * ?????????????????????????????????????????????????????????
             */
            if (switcherManager.isAllSwitchOn(moduleSpec.getDependencies())) {
                loadModule(moduleSpec);
                if (isInfoEnabled) {
                    logger.info("SIMULATOR: load module {} successful,file={}", moduleSpec.getModuleId(), moduleSpec.getFile().getAbsolutePath());
                }
            } else {
                switcherManager.registerMultiSwitchOnCallback(moduleSpec.getDependencies(), new Runnable() {
                    @Override
                    public void run() {
                        /**
                         * ??????????????????????????????
                         */
                        if (switcherManager.isAllSwitchOn(moduleSpec.getDependencies())) {
                            loadModule(moduleSpec);
                            if (isInfoEnabled) {
                                logger.info("SIMULATOR: load module {} successful,file={}", moduleSpec.getModuleId(), moduleSpec.getFile().getAbsolutePath());
                            }
                        } else {
                            /**
                             * ????????????????????????,????????????????????????????????????
                             */
                            switcherManager.registerMultiSwitchOnCallback(moduleSpec.getDependencies(), this);
                        }
                    }
                });
            }


        } else {
            loadModule(moduleSpec);
            if (isInfoEnabled) {
                logger.info("SIMULATOR: load module {} successful,file={}", moduleSpec.getModuleId(), moduleSpec.getFile().getAbsolutePath());
            }
        }
    }

    protected void loadModules(List<ModuleSpec> moduleSpecs, String action) {
        for (ModuleSpec moduleSpec : moduleSpecs) {
            loadModule(moduleSpec, action);
        }
    }

    @Override
    public CoreModuleManager load(File file) throws ModuleException {
        ModuleSpec moduleSpec = ModuleSpecUtils.loadModuleSpec(file, false);
        loadModule(moduleSpec, "load");
        return this;
    }

    @Override
    public synchronized CoreModuleManager reset() throws ModuleException {
        if (isInfoEnabled) {
            logger.info("SIMULATOR: resetting all loaded modules:{}", loadedModuleMap.keySet());
        }

        waitLoadModules.clear();

        // 1. ????????????????????????
        unloadAll();

        // 2. ??????????????????????????????
        List<File> systemModuleLibJars = getAllModuleLibJar(systemModuleLibs);
        List<ModuleSpec> systemModuleSpecs = ModuleSpecUtils.loadModuleSpecs(systemModuleLibJars, true, true);

        // 3. ?????????????????????????????????, ???????????????????????????????????????????????????
        List<File> userModuleLibJars = getAllModuleLibJar(userModuleLibs);
        List<ModuleSpec> userModuleSpecs = ModuleSpecUtils.loadModuleSpecs(userModuleLibJars, false, true);
        initAllModuleInfos(systemModuleSpecs);
        initAllModuleInfos(userModuleSpecs);

        //??????
        loadModules(systemModuleSpecs, "load");
        loadModules(userModuleSpecs, "load");
        if (isInfoEnabled) {
            logger.info("SIMULATOR: resetting all loaded modules finished :{}", loadedModuleMap.keySet());
        }
        return this;
    }

    /**
     * ?????????????????????????????????
     */
    protected void initAllModuleInfos(List<ModuleSpec> moduleSpecList) {
        for (ModuleSpec moduleSpec : moduleSpecList) {
            ModuleLoadInfo moduleLoadInfo = new ModuleLoadInfo();
            moduleLoadInfo.setModuleId(moduleSpec.getModuleId());
            moduleLoadInfo.setStatus(ModuleLoadStatusEnum.UNLOAD);
            DefaultModuleLoadInfoManagerUtils.getDefaultModuleLoadInfoManager().addModuleLoadInfo(moduleLoadInfo);
        }
    }

    /**
     * ??????ModuleJarClassLoader
     * ???ModuleJarClassLoader?????????????????????????????????????????????????????????ClassLoader????????????????????????
     *
     * @param classLoaderFactory ??????????????????ClassLoader??????
     */
    private void closeModuleJarClassLoaderIfNecessary(final ClassLoaderFactory classLoaderFactory) {
        // ??????????????????????????????????????????????????????ModuleClassLoader?????????
        boolean hasRef = false;
        for (final CoreModule coreModule : loadedModuleMap.values()) {
            if (classLoaderFactory == coreModule.getClassLoaderFactory()) {
                hasRef = true;
                break;
            }
        }

        if (!hasRef) {
            if (isInfoEnabled) {
                logger.info("SIMULATOR: ModuleClassLoaderFactory={} will be close: all module unloaded.", classLoaderFactory);
            }
            classLoaderFactory.release();
        }

    }


    private boolean isChecksumCRC32Existed(long checksumCRC32) {
        for (final CoreModule coreModule : loadedModuleMap.values()) {
            if (coreModule.getClassLoaderFactory().getChecksumCRC32() == checksumCRC32) {
                return true;
            }
        }
        return false;
    }

    /**
     * ?????????
     * ?????????????????????????????????????????????????????????????????????????????????
     */
    private void softFlush() {
        if (isInfoEnabled) {
            logger.info("SIMULATOR: soft-flushing modules:{}", loadedModuleMap.keySet());
        }

        final File systemModuleLibDir = new File(config.getSystemModuleLibPath());
        try {
            final ArrayList<File> appendJarFiles = new ArrayList<File>();
            final ArrayList<CoreModule> removeCoreModules = new ArrayList<CoreModule>();
            final ArrayList<Long> checksumCRC32s = new ArrayList<Long>();

            // 1. ??????????????????????????????(add/remove)
            for (final File jarFile : this.moduleLoader.loadModuleLibs(config.getAppName(), config.getUserModulePaths())) {
                final long checksumCRC32;
                try {
                    checksumCRC32 = FileUtils.checksumCRC32(jarFile);
                } catch (IOException cause) {
                    logger.warn("SIMULATOR: soft-flushing module: compute module-jar CRC32 occur error. module-jar={};", jarFile, cause);
                    continue;
                }
                checksumCRC32s.add(checksumCRC32);
                // ??????CRC32???????????????????????????????????????????????????????????????????????????????????????
                if (isChecksumCRC32Existed(checksumCRC32)) {
                    if (isInfoEnabled) {
                        logger.info("SIMULATOR: soft-flushing module: module-jar is not changed, ignored. module-jar={};CRC32={};", jarFile, checksumCRC32);
                    }
                    continue;
                }

                if (isInfoEnabled) {
                    logger.info("SIMULATOR: soft-flushing module: module-jar is changed, will be flush. module-jar={};CRC32={};", jarFile, checksumCRC32);
                }
                appendJarFiles.add(jarFile);
            }

            // 2. ?????????????????????????????????????????????
            for (final CoreModule coreModule : loadedModuleMap.values()) {
                final ClassLoaderFactory classLoaderFactory = coreModule.getClassLoaderFactory();

                // ????????????????????????????????????
                if (isOptimisticDirectoryContainsFile(systemModuleLibDir, coreModule.getJarFile())) {
                    if (isDebugEnabled) {
                        logger.debug("SIMULATOR: soft-flushing module: module-jar is in system-lib, will be ignored. module-jar={};system-lib={};",
                                coreModule.getJarFile(),
                                systemModuleLibDir
                        );
                    }
                    continue;
                }

                // ??????CRC32?????????????????????????????????????????????????????????????????????????????????
                if (checksumCRC32s.contains(classLoaderFactory.getChecksumCRC32())) {
                    if (isInfoEnabled) {
                        logger.info("SIMULATOR: soft-flushing module: module-jar already loaded, ignored. module-jar={};CRC32={};",
                                coreModule.getJarFile(),
                                classLoaderFactory.getChecksumCRC32()
                        );
                    }
                    continue;
                }
                if (isInfoEnabled) {
                    logger.info("SIMULATOR: soft-flushing module: module-jar is changed, module will be reload/remove. module={};module-jar={};",
                            coreModule.getModuleId(),
                            coreModule.getJarFile()
                    );
                }
                removeCoreModules.add(coreModule);
            }

            // 3. ??????remove
            for (final CoreModule coreModule : removeCoreModules) {
                unload(coreModule, true);
            }

            // 3. ?????????????????????????????????
            List<ModuleSpec> userModuleSpecs = ModuleSpecUtils.loadModuleSpecs(appendJarFiles, false, true);
            loadModules(userModuleSpecs, "soft-flush");
        } catch (Throwable cause) {
            logger.warn("SIMULATOR: soft-flushing modules: occur error.", cause);
        }

    }

    /**
     * ????????????
     * ?????????????????????????????????????????????????????????????????????
     *
     * @throws ModuleException ??????????????????
     */
    private void forceFlush() throws ModuleException {
        if (isInfoEnabled) {
            logger.info("SIMULATOR: force-flushing modules:{}", loadedModuleMap.keySet());
        }

        // 1. ????????????
        // ???????????????????????????
        final Collection<CoreModule> waitingUnloadCoreModules = new ArrayList<CoreModule>();

        // ????????????USER??????????????????????????????????????????
        for (final CoreModule coreModule : loadedModuleMap.values()) {
            // ?????????????????????USER???????????????????????????????????????????????????????????????????????????????????????
            if (!isSystemModule(coreModule.getJarFile())) {
                waitingUnloadCoreModules.add(coreModule);
            }
        }

        // ?????????????????????????????????ID??????
        if (isInfoEnabled) {
            final Set<String> moduleIds = new LinkedHashSet<String>();
            for (final CoreModule coreModule : waitingUnloadCoreModules) {
                moduleIds.add(coreModule.getModuleId());
            }
            if (isInfoEnabled) {
                logger.info("SIMULATOR: force-flush modules: will be unloading modules : {}", moduleIds);
            }
        }

        // ????????????????????????????????????????????????????????????
        for (final CoreModule coreModule : waitingUnloadCoreModules) {
            unload(coreModule, true);
        }

        // 2. ????????????
        // ?????????????????????????????????????????????????????????????????????
        // ?????????????????????????????????
        // ??????????????????
        List<File> userModuleLibJars = getAllModuleLibJar(userModuleLibs);
        List<ModuleSpec> userModuleSpecs = ModuleSpecUtils.loadModuleSpecs(userModuleLibJars, false, true);
        loadModules(userModuleSpecs, "force-flush");

    }

}
