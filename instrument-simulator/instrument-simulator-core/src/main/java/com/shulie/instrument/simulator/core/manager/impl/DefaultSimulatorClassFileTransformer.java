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
package com.shulie.instrument.simulator.core.manager.impl;

import com.shulie.instrument.simulator.api.guard.SimulatorGuard;
import com.shulie.instrument.simulator.api.listener.EventListener;
import com.shulie.instrument.simulator.api.listener.ext.BuildingForListeners;
import com.shulie.instrument.simulator.core.CoreModule;
import com.shulie.instrument.simulator.core.enhance.EventEnhancer;
import com.shulie.instrument.simulator.core.manager.AffectStatistic;
import com.shulie.instrument.simulator.core.manager.SimulatorClassFileTransformer;
import com.shulie.instrument.simulator.core.util.LogbackUtils;
import com.shulie.instrument.simulator.core.util.SimulatorClassUtils;
import com.shulie.instrument.simulator.core.util.matcher.Matcher;
import com.shulie.instrument.simulator.core.util.matcher.MatchingResult;
import com.shulie.instrument.simulator.core.util.matcher.UnsupportedMatcher;
import com.shulie.instrument.simulator.core.util.matcher.structure.ClassStructure;
import com.shulie.instrument.simulator.core.util.matcher.structure.ClassStructureFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/18 9:02 下午
 */
public class DefaultSimulatorClassFileTransformer extends SimulatorClassFileTransformer {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final boolean isDebugEnabled = logger.isDebugEnabled();
    private final boolean isInfoEnabled = logger.isInfoEnabled();
    private final static int CLASS_VERSION_15 = 49;
    private final static byte CLASS_VERSION_15_6 = 0;
    private final static byte CLASS_VERSION_15_7 = 49;

    private final int watchId;
    private final String moduleId;
    private final Matcher matcher;
    private final boolean isEnableUnsafe;

    private final AffectStatistic affectStatistic = new AffectStatistic();
    private final Map<Integer, EventListener> eventListeners = new HashMap<Integer, EventListener>();
    private final List<BuildingForListeners> listeners;
    private final DefaultModuleEventWatcher watcher;

    DefaultSimulatorClassFileTransformer(final DefaultModuleEventWatcher watcher,
                                         final int watchId,
                                         final CoreModule coreModule,
                                         final Matcher matcher,
                                         final boolean isEnableUnsafe) {
        this.watcher = watcher;
        this.watchId = watchId;
        this.moduleId = coreModule.getModuleId();
        this.matcher = matcher;
        this.isEnableUnsafe = isEnableUnsafe;
        List<BuildingForListeners> listeners = matcher.getAllListeners();
        for (BuildingForListeners listener : listeners) {
            eventListeners.put(listener.getListenerId(), new LazyEventListenerProxy(coreModule, listener.getListeners()));
        }
        this.listeners = matcher.getAllListeners();
    }

    @Override
    public Map<Integer, EventListener> getEventListeners() {
        return eventListeners;
    }

    @Override
    public List<BuildingForListeners> getAllListeners() {
        return listeners;
    }

    // 获取当前类结构
    private ClassStructure getClassStructure(final ClassLoader loader,
                                             final Class<?> classBeingRedefined,
                                             final byte[] srcByteCodeArray) {
        return null == classBeingRedefined
                ? ClassStructureFactory.createClassStructure(srcByteCodeArray, loader)
                : ClassStructureFactory.createClassStructure(classBeingRedefined);
    }

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String internalClassName,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] srcByteCodeArray) {
        SimulatorGuard.getInstance().enter();
        try {

            // 这里过滤掉Simulator所需要的类|来自SimulatorClassLoader所加载的类|来自ModuleJarClassLoader加载的类
            // 防止ClassCircularityError的发生
            if (SimulatorClassUtils.isComeFromSimulatorFamily(internalClassName, loader)) {
                return null;
            }

            return _transform(
                    loader,
                    internalClassName,
                    classBeingRedefined,
                    srcByteCodeArray
            );


        } catch (Throwable cause) {
            logger.warn("SIMULATOR: simulator transform {} in loader={}; failed, module={} at watch={}, will ignore this transform.",
                    internalClassName,
                    loader,
                    moduleId,
                    watchId,
                    cause
            );
            return null;
        } finally {
            SimulatorGuard.getInstance().exit();
        }
    }

    private byte[] _transform(final ClassLoader loader,
                              String internalClassName,
                              final Class<?> classBeingRedefined,
                              byte[] srcByteCodeArray) {
        long start = System.currentTimeMillis();
        long l = System.currentTimeMillis();
        // 如果未开启unsafe开关，是不允许增强来自BootStrapClassLoader的类
        try {
            if (!isEnableUnsafe
                    && null == loader) {
                if (isDebugEnabled) {
                    logger.debug("SIMULATOR: transform ignore {}, class from bootstrap but unsafe.enable=false.", internalClassName);
                }
                return null;
            }

            ClassStructure classStructure =null;
            if (internalClassName == null) {
                classStructure = getClassStructure(loader, classBeingRedefined, srcByteCodeArray);
                internalClassName = classStructure.getJavaClassName();
            }
            l = costTimePrint(internalClassName, "internalClassName", l);
            if (!matcher.preMatching(internalClassName.replace('/', '.'))) {
                if (isDebugEnabled) {
                    logger.debug("SIMULATOR: transform ignore {}, classname is not matched!", internalClassName, loader);
                }
                l = costTimePrint(internalClassName, "preNoMatch", l);
                return null;
            }

            if (classStructure == null) {
                classStructure = getClassStructure(loader, classBeingRedefined, srcByteCodeArray);
            }
            l = costTimePrint(internalClassName, "getClassStructure", l);
            final MatchingResult matchingResult = new UnsupportedMatcher(loader, isEnableUnsafe).and(matcher).matching(classStructure);
            final Map<String, Set<BuildingForListeners>> behaviorSignCodes = matchingResult.getBehaviorSignCodeMap();

            // 如果一个行为都没匹配上也不用继续了
            if (!matchingResult.isMatched() || behaviorSignCodes.isEmpty()) {
                if (isDebugEnabled) {
                    logger.debug("SIMULATOR: transform ignore {}, no behaviors matched in loader={}", internalClassName, loader);
                }
                l = costTimePrint(internalClassName, "noMatch", l);
                return null;
            }

            /**
             * ASM增强中使用到了 LDC 命令，LDC命令在 java5(49)才支持，所以当类版本低于此版本时，强制将类版本设置为 java5(49)
             */
            if (getClassMajorVersion(srcByteCodeArray) < CLASS_VERSION_15) {
                srcByteCodeArray = resetClassVersionToJava5(srcByteCodeArray);
            }
            l = costTimePrint(internalClassName, "matched", l);

            // 开始进行类匹配
            try {
                byte[] toByteCodeArray = new EventEnhancer().toByteCodeArray(
                        loader,
                        srcByteCodeArray,
                        behaviorSignCodes
                );
                l = costTimePrint(internalClassName, "toByteCode", l);

                if (srcByteCodeArray == toByteCodeArray) {
                    if (isDebugEnabled) {
                        logger.debug("SIMULATOR: transform ignore {}, nothing changed in loader={}", internalClassName, loader);
                    }
                    return null;
                }
                // statistic affect
                affectStatistic.statisticAffect(loader, internalClassName, behaviorSignCodes);

                if (isInfoEnabled) {
                    logger.info("SIMULATOR:[tcf]cost {} transform {} finished, by module={} in loader={}", (System.currentTimeMillis() - start), internalClassName, moduleId, loader);
                }
                l = costTimePrint(internalClassName, "toFinish", l);
                return toByteCodeArray;
            } catch (Throwable cause) {
                logger.warn("SIMULATOR: transform {} failed, by module={} in loader={}", internalClassName, moduleId, loader, cause);
                return null;
            }
        } finally {
            costTimePrint(internalClassName, "transform", start);
        }
    }

    private long costTimePrint(String cn, String name, long startTime) {
        LogbackUtils.costTimePrint("transform", cn, name, startTime);
        return System.currentTimeMillis();
    }

    /**
     * 获取 class 的编译的平台版本号
     * 46 -> 1.2
     * 47 -> 1.3
     * 48 -> 1.4
     * 49 -> 1.5
     * 50 -> 1.6
     * 51 -> 1.7
     * 52 -> 1.8
     *
     * @param data
     * @return
     */
    private int getClassMajorVersion(byte[] data) {
        return (short) (((data[6] & 0xFF) << 8) | (data[6 + 1] & 0xFF));
    }

    /**
     * 将版本号设置为 java5
     *
     * @param data
     * @return
     */
    private byte[] resetClassVersionToJava5(byte[] data) {
        data[6] = CLASS_VERSION_15_6;
        data[7] = CLASS_VERSION_15_7;
        return data;
    }

    /**
     * 获取观察ID
     *
     * @return 观察ID
     */
    @Override
    public int getWatchId() {
        return watchId;
    }


    /**
     * 获取本次匹配器
     *
     * @return 匹配器
     */
    @Override
    public Matcher getMatcher() {
        return matcher;
    }

    /**
     * 获取本次增强的影响统计
     *
     * @return 本次增强的影响统计
     */
    @Override
    public AffectStatistic getAffectStatistic() {
        return affectStatistic;
    }
}
