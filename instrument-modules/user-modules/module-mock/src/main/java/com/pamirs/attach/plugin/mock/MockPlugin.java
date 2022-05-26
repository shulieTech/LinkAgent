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
package com.pamirs.attach.plugin.mock;

import com.pamirs.attach.plugin.mock.interceptor.MockAdviceListener;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.internal.config.MockConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MockConfigAddEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MockConfigModifyEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.MockConfigRemoveEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.listener.ext.EventWatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.EventWatcher;
import com.shulie.instrument.simulator.api.listener.ext.IBehaviorMatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.IClassMatchBuilder;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = MockConstants.MODULE_NAME, version = "1.0.0", author = "xiaobin@shulie.io",
        description = "mock挡板,使用 groovy 脚本编写 mock 内容,以方法为维度，支持方法精确到参数指定,如 com.shulie.test.Test#doTest(int,java.lang.String)")
public class MockPlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private final static Logger LOGGER = LoggerFactory.getLogger(MockPlugin.class);
    private Map<String, EventWatcher> watchers;

    @Override
    public boolean onActive() throws Throwable {
        try {
            this.watchers = new Hashtable<String, EventWatcher>();
            EventRouter.router().addListener(new PradarEventListener() {
                @Override
                public EventResult onEvent(IEvent event) {
                    synchronized (LOGGER) {
                        if (event instanceof MockConfigAddEvent) {
                            Set<MockConfig> mockConfigs = ((MockConfigAddEvent) event).getTarget();
                            Set<MockConfig> newConfigs = new HashSet<MockConfig>();
                            for (MockConfig mockConfig : mockConfigs) {
                                if (watchers.containsKey(mockConfig.getKey())) {
                                    continue;
                                }
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("add mock config interceptor {}", mockConfig.getKey());
                                }
                                newConfigs.add(mockConfig);
                            }
                            Map<String, Set<MockConfig>> map = groupByClass(newConfigs);
                            watchers.putAll(enhanceClassMethod(map));
                        } else if (event instanceof MockConfigRemoveEvent) {
                            Set<MockConfig> mockConfigs = ((MockConfigRemoveEvent) event).getTarget();
                            for (MockConfig mockConfig : mockConfigs) {
                                EventWatcher watcher = watchers.remove(mockConfig.getKey());
                                if (watcher != null) {
                                    if (LOGGER.isInfoEnabled()) {
                                        LOGGER.info("Remove mock config interceptor {}", mockConfig.getKey());
                                    }
                                    watcher.onUnWatched();
                                }
                            }
                        } else if (event instanceof MockConfigModifyEvent) {
                            Set<MockConfig> mockConfigs = ((MockConfigModifyEvent) event).getTarget();
                            for (MockConfig mockConfig : mockConfigs) {
                                EventWatcher watcher = watchers.remove(mockConfig.getKey());
                                if (watcher != null) {
                                    if (LOGGER.isInfoEnabled()) {
                                        LOGGER.info("Modify mock config pre remove interceptor  {}", mockConfig.getKey());
                                    }
                                    watcher.onUnWatched();
                                }
                            }
                            Map<String, Set<MockConfig>> map = groupByClass(mockConfigs);
                            watchers.putAll(enhanceClassMethod(map));
                        } else {
                            return EventResult.IGNORE;
                        }

                        return EventResult.success("Mock config update successful.");
                    }
                }

                @Override
                public int order() {
                    return -1;
                }
            });

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(60000);
                            synchronized (LOGGER) {
                                Set<MockConfig> mockConfigs = new HashSet<MockConfig>();
                                HashSet<MockConfig> set = new HashSet<MockConfig>(GlobalConfig.getInstance().getMockConfigs());
                                for (MockConfig mockConfig : set) {
                                    if (watchers.containsKey(mockConfig.getKey())) {
                                        continue;
                                    }
                                    mockConfigs.add(mockConfig);
                                }
                                if (CollectionUtils.isNotEmpty(mockConfigs)) {
                                    Map<String, Set<MockConfig>> map = groupByClass(mockConfigs);
                                    watchers.putAll(enhanceClassMethod(map));
                                }
                            }
                        } catch (Throwable throwable) {
                            LOGGER.warn("try mock catch error", throwable);
                        }
                    }

                }
            });
            thread.setName("simulator_module_mockInit");
            thread.start();

        } catch (Throwable e) {
            LOGGER.warn("挡板增强失败", e);
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.LinkGuardEnhance)
                    .setErrorCode("mock-enhance-0001")
                    .setMessage("挡板增强失败！")
                    .setDetail("挡板增强失败:" + e.getMessage())
                    .closePradar(ConfigNames.LINK_GUARD_CONFIG)
                    .report();
            throw e;
        }
        return true;
    }

    private Map<String, Set<MockConfig>> groupByClass(Set<MockConfig> mockConfigs) {
        Map<String, Set<MockConfig>> map = new HashMap<String, Set<MockConfig>>();
        for (MockConfig mockConfig : mockConfigs) {
            Set<MockConfig> configs = map.get(mockConfig.getClassName());
            if (configs == null) {
                configs = new HashSet<MockConfig>();
                map.put(mockConfig.getClassName(), configs);
            }
            configs.add(mockConfig);
        }
        return map;
    }


    public Map<String, EventWatcher> enhanceClassMethod(Map<String, Set<MockConfig>> configs) {
        if (configs == null || configs.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        Map<String, EventWatcher> watchers = new HashMap<String, EventWatcher>();
        for (Map.Entry<String, Set<MockConfig>> entry : configs.entrySet()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("pre enhance class:{} ,configs:{}", entry.getKey(), entry.getValue());
            }
            for (MockConfig mockConfig : entry.getValue()) {
                IClassMatchBuilder buildingForClass = new EventWatchBuilder(moduleEventWatcher).onClass(entry.getKey());
                IBehaviorMatchBuilder buildingForBehavior = buildingForClass.onAnyBehavior(mockConfig.getMethodName());
                if (mockConfig.getMethodArgClasses() != null && !mockConfig.getMethodArgClasses().isEmpty()) {
                    buildingForBehavior.withParameterTypes(mockConfig.getMethodArgClasses().toArray(new String[mockConfig.getMethodArgClasses().size()]));
                }
                buildingForBehavior.onListener(Listeners.of(MockAdviceListener.class, new Object[]{mockConfig.getCodeScript()}));
                watchers.put(mockConfig.getKey(), buildingForClass.onWatch());
            }
        }
        return watchers;
    }

    @Override
    public void onFrozen() throws Throwable {
        if (this.watchers != null) {
            for (Map.Entry<String, EventWatcher> entry : this.watchers.entrySet()) {
                entry.getValue().onUnWatched();
            }
            this.watchers.clear();
        }
    }

    @Override
    public void onUnload() throws Throwable {
        this.watchers = null;
    }
}
