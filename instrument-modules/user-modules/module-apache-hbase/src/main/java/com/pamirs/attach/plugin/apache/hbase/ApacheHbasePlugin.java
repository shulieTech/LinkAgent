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
package com.pamirs.attach.plugin.apache.hbase;

import com.pamirs.attach.plugin.apache.hbase.interceptor.ConnectionShadowInterceptor;
import com.pamirs.attach.plugin.apache.hbase.interceptor.HConnectionShadowReplaceInterceptor;
import com.pamirs.attach.plugin.apache.hbase.interceptor.TableCutoffInterceptor;
import com.pamirs.attach.plugin.common.datasource.hbaseserver.MediatorConnection;
import com.pamirs.pradar.interceptor.Interceptors;
import com.pamirs.pradar.internal.config.ShadowHbaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowHbaseDisableEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowHbaseDynamicEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.instrument.EnhanceCallback;
import com.shulie.instrument.simulator.api.instrument.InstrumentClass;
import com.shulie.instrument.simulator.api.instrument.InstrumentMethod;
import com.shulie.instrument.simulator.api.listener.Listeners;
import com.shulie.instrument.simulator.api.scope.ExecutionPolicy;
import org.kohsuke.MetaInfServices;

/**
 * apache-hbase 影子库
 *
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.hbase
 * @Date 2021/4/27 10:58 上午
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "apache-hbase", version = "1.0.0", author = "xiaobin@shulie.io", description = "支持apache hbase")
public class ApacheHbasePlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    private PradarEventListener dynamicEventListener;
    private PradarEventListener disableEventListener;

    @Override
    public boolean onActive() throws Throwable {
        this.enhanceTemplate.enhance(this, "org.apache.hadoop.hbase.client.ConnectionFactory", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                final InstrumentMethod connectionMethod = target.getDeclaredMethod("createConnection",
                        "org.apache.hadoop.conf.Configuration", "java.util.concurrent.ExecutorService",
                        "org.apache.hadoop.hbase.security.User");
                connectionMethod.addInterceptor(
                        Listeners.of(ConnectionShadowInterceptor.class, "hbase", ExecutionPolicy.BOUNDARY,
                                Interceptors.SCOPE_CALLBACK));
            }

        });

        this.enhanceTemplate.enhance(this, "org.apache.hadoop.hbase.client.ConnectionManager$HConnectionImplementation",
                new EnhanceCallback() {
                    @Override
                    public void doEnhance(InstrumentClass target) {
                        final InstrumentMethod connectionMethod = target.getDeclaredMethods("isMasterRunning",
                                "isTableAvailable", "locateRegion", "clearRegionCache", "cacheLocation",
                                "deleteCachedRegionLocation", "relocateRegion", "updateCachedLocations", "locateRegions",
                                "getMaster", "getAdmin", "getClient", "getRegionLocation", "clearCaches",
                                "getKeepAliveMasterService", "isDeadServer", "getNonceGenerator", "getAsyncProcess",
                                "getNewRpcRetryingCallerFactory", "getRpcRetryingCallerFactory", "getRpcControllerFactory",
                                "getConnectionConfiguration", "isManaged", "getStatisticsTracker", "getBackoffPolicy",
                                "getConnectionMetrics", "hasCellBlockSupport", "getConfiguration", "getTable", "getRegionLocator",
                                "isTableEnabled", "isTableDisabled", "listTables", "getTableNames", "listTableNames",
                                "getHTableDescriptor", "processBatch", "processBatchCallback", "setRegionCachePrefetch",
                                "getRegionCachePrefetch", "getCurrentNrHRS", "getHTableDescriptorsByTableName",
                                "getHTableDescriptors", "isClosed", "getBufferedMutator", "close");
                        connectionMethod.addInterceptor(
                                Listeners.of(HConnectionShadowReplaceInterceptor.class, "hbase", ExecutionPolicy.BOUNDARY,
                                        Interceptors.SCOPE_CALLBACK));
                    }

                });

        enhanceTemplate.enhance(this, "org.apache.hadoop.hbase.client.HTable", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass target) {
                InstrumentMethod getMethod = target.getDeclaredMethod("get", "org.apache.hadoop.hbase.client.Get");
                getMethod.addInterceptor(Listeners.of(TableCutoffInterceptor.class, "hbase", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod method = target.getDeclaredMethods("append", "increment", "exists", "existsAll", "getScanner", "put", "checkAndPut", "delete", "checkAndDelete", "mutateRow", "checkAndMutate");
                method.addInterceptor(Listeners.of(TableCutoffInterceptor.class, "hbase", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));
            }
        });

        ignoredTypesBuilder.ignoreClass("org.apache.hadoop.hbase.");

        initEventListener();
        return true;
    }

    void initEventListener() {
        dynamicEventListener = new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (!ShadowHbaseDynamicEvent.class.isAssignableFrom(event.getClass())) {
                    return EventResult.IGNORE;
                }

                if (!GlobalConfig.getInstance().isShadowHbaseServer()) {
                    return EventResult.IGNORE;
                }

                try {
                    MediatorConnection.dynamic((ShadowHbaseConfig) event.getTarget());
                } catch (Exception e) {
                }
                return EventResult.success(event.getTarget());
            }

            @Override
            public int order() {
                return 10;
            }
        };
        EventRouter.router().addListener(dynamicEventListener);

        disableEventListener = new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (!ShadowHbaseDisableEvent.class.isAssignableFrom(event.getClass())) {
                    return EventResult.IGNORE;
                }
                try {
                    MediatorConnection.disable((String) event.getTarget());
                } catch (Exception e) {
                }
                return EventResult.success(event.getTarget());
            }

            @Override
            public int order() {
                return 11;
            }
        };
        EventRouter.router().addListener(disableEventListener);
    }

    @Override
    public void onFrozen() throws Throwable {
        if (dynamicEventListener != null) {
            EventRouter.router().removeListener(dynamicEventListener);
        }
        if (disableEventListener != null) {
            EventRouter.router().removeListener(disableEventListener);
        }
    }

    @Override
    public void onUnload() throws Throwable {
        dynamicEventListener = null;
        disableEventListener = null;
    }
}
