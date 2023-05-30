package com.pamirs.attach.plugin.cus.trace;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.cus.trace.interceptor.CusTraceInterceptor;
import com.pamirs.attach.plugin.cus.trace.module.CusTraceConfig;
import com.pamirs.pradar.interceptor.Interceptors;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.listener.ext.EventWatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.EventWatcher;
import com.shulie.instrument.simulator.api.listener.ext.IBehaviorMatchBuilder;
import com.shulie.instrument.simulator.api.listener.ext.IClassMatchBuilder;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Licey
 * @date 2022/12/19
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "cus-trace", version = "1.0.0", author = "langyi@shulie.io", description = "指定方法采集trace,如 io.shulie.agent.test.Test#doTest(int,java.lang.String)")
public class CusTracePlugin extends ModuleLifecycleAdapter implements ExtensionModule {
    private static final Logger logger = LoggerFactory.getLogger(CusTracePlugin.class);

    private Map<String, EventWatcher> watchers = new ConcurrentHashMap<>();

    private boolean stop = false;

    private String oldCusTraceConfig = "";

    @Override
    public boolean onActive() throws Throwable {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!stop) {
                    try {
                        String cusTraceConfig = GlobalConfig.getInstance().getSimulatorDynamicConfig().cusTraceConfig();
                        if ((StringUtil.isEmpty(oldCusTraceConfig) && StringUtil.isEmpty(cusTraceConfig))
                                || oldCusTraceConfig.equals(cusTraceConfig)) {
                            continue;
                        }
                        final Set<CusTraceConfig> oldConfigSet = decodeConfig(oldCusTraceConfig);
                        Set<CusTraceConfig> newConfigSet = decodeConfig(cusTraceConfig);

                        oldCusTraceConfig = cusTraceConfig;

                        Set<CusTraceConfig> toRemoveConfig = new HashSet<>();
                        Set<CusTraceConfig> toNewConfig = new HashSet<>();

                        for (CusTraceConfig c : newConfigSet) {
                            if (!oldConfigSet.contains(c)) {
                                toNewConfig.add(c);
                            }
                        }

                        for (CusTraceConfig c : oldConfigSet) {
                            if (!newConfigSet.contains(c)) {
                                toRemoveConfig.add(c);
                            }
                        }

                        unEnhanceClassMethod(toRemoveConfig);
                        enhanceClassMethod(toNewConfig);
                    } catch (Exception e) {
                        logger.error("[cus-trace] error", e);
                    } finally {
                        try {
                            TimeUnit.MINUTES.sleep(1);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
            }
        });
        thread.setName("cus-thread");
        thread.start();
//        PradarConfig.register("cus.trace.config", new PradarConfig.PradarConfigChangeCallback() {
//            @Override
//            public void change(Config config, Config old) {
//                final Set<CusTraceConfig> oldConfigSet = decodeConfig(old);
//                Set<CusTraceConfig> newConfigSet = decodeConfig(config);
//
//                Set<CusTraceConfig> toRemoveConfig = new HashSet<>();
//                Set<CusTraceConfig> toNewConfig = new HashSet<>();
//
//                for (CusTraceConfig c : newConfigSet) {
//                    if (!oldConfigSet.contains(c)) {
//                        toNewConfig.add(c);
//                    }
//                }
//
//                for (CusTraceConfig c : oldConfigSet) {
//                    if (!newConfigSet.contains(c)) {
//                        toRemoveConfig.add(c);
//                    }
//                }
//
//                unEnhanceClassMethod(toRemoveConfig);
//                enhanceClassMethod(toNewConfig);
//            }
//        });
        return true;
    }

    private Set<CusTraceConfig> decodeConfig(String config) {
        Set<CusTraceConfig> configSet = new HashSet<>();
        if (!StringUtil.isEmpty(config)) {
            configSet.addAll(JSON.parseArray(config, CusTraceConfig.class));
        }
        return configSet;
    }

    private void unEnhanceClassMethod(Set<CusTraceConfig> configSet) {
        for (CusTraceConfig config : configSet) {
            EventWatcher remove = watchers.remove(config.getKey());
            if (remove != null) {
                logger.info("unEnhance {}", config.getKey());
                remove.onUnWatched();
            } else {
                logger.warn("can not found to remove key {}, maybe removed", config.getKey());
            }
        }
    }

//    private void updateTracePoint() {
//        if (event instanceof MockConfigAddEvent) {
//            Set<MockConfig> mockConfigs = ((MockConfigAddEvent) event).getTarget();
//            Set<MockConfig> newConfigs = new HashSet<MockConfig>();
//            for (MockConfig mockConfig : mockConfigs) {
//                if (watchers.containsKey(mockConfig.getKey())) {
//                    continue;
//                }
//                newConfigs.add(mockConfig);
//            }
//            Map<String, Set<MockConfig>> map = groupByClass(newConfigs);
//            watchers.putAll(enhanceClassMethod(map));
//        } else if (event instanceof MockConfigRemoveEvent) {
//            Set<MockConfig> mockConfigs = ((MockConfigRemoveEvent) event).getTarget();
//            for (MockConfig mockConfig : mockConfigs) {
//                EventWatcher watcher = watchers.remove(mockConfig.getKey());
//                if (watcher != null) {
//                    if (logger.isInfoEnabled()) {
//                        logger.info("Remove mock config interceptor {}", mockConfig.getKey());
//                    }
//                    watcher.onUnWatched();
//                }
//            }
//        } else if (event instanceof MockConfigModifyEvent) {
//            Set<MockConfig> mockConfigs = ((MockConfigModifyEvent) event).getTarget();
//            for (MockConfig mockConfig : mockConfigs) {
//                EventWatcher watcher = watchers.remove(mockConfig.getKey());
//                if (watcher != null) {
//                    if (logger.isInfoEnabled()) {
//                        logger.info("Modify mock config pre remove interceptor  {}", mockConfig.getKey());
//                    }
//                    watcher.onUnWatched();
//                }
//            }
//            Map<String, Set<MockConfig>> map = groupByClass(mockConfigs);
//            watchers.putAll(enhanceClassMethod(map));
//        } else {
//            return EventResult.IGNORE;
//        }
//
//        return EventResult.success("Mock config update successful.");
//    }


    public Map<String, EventWatcher> enhanceClassMethod(Set<CusTraceConfig> traceConfigSet) {
        if (traceConfigSet == null || traceConfigSet.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        for (CusTraceConfig cusTraceConfig : traceConfigSet) {
            if (logger.isInfoEnabled()) {
                logger.info("pre enhance class#methos:{} ", cusTraceConfig.getKey());
            }

            IClassMatchBuilder buildingForClass = new EventWatchBuilder(moduleEventWatcher).onClass(cusTraceConfig.getClassName());
            buildingForClass.needReTransform();
            IBehaviorMatchBuilder buildingForBehavior = buildingForClass.onAnyBehavior(cusTraceConfig.getMethodName());
            if (cusTraceConfig.getMethodArgClasses() != null && !cusTraceConfig.getMethodArgClasses().isEmpty()) {
                buildingForBehavior.withParameterTypes(cusTraceConfig.getMethodArgClasses().toArray(new String[0]));
            }
            buildingForBehavior.onListener(Listeners.of(CusTraceInterceptor.class, cusTraceConfig.getKey(), ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK, new Object[]{cusTraceConfig}));
            watchers.put(cusTraceConfig.getKey(), buildingForClass.onWatch());

        }
        return watchers;
    }

    @Override
    public void onFrozen() throws Throwable {
        this.stop = true;
        if (this.watchers != null) {
            for (Map.Entry<String, EventWatcher> entry : this.watchers.entrySet()) {
                entry.getValue().onUnWatched();
            }
            this.watchers.clear();
        }
    }

    @Override
    public void onUnload() throws Throwable {
        this.stop = true;
        this.watchers = null;
    }
}
