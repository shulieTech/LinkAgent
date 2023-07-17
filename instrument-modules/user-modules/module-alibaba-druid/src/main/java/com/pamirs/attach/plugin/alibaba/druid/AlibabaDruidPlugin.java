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
package com.pamirs.attach.plugin.alibaba.druid;

import com.pamirs.attach.plugin.alibaba.druid.interceptor.DruidGetConnectionDirectInterceptor;
import com.pamirs.attach.plugin.alibaba.druid.interceptor.DruidInjectGetConnectionInterceptor;
import com.pamirs.attach.plugin.alibaba.druid.listener.DruidShadowActiveEventListener;
import com.pamirs.attach.plugin.alibaba.druid.listener.DruidShadowDisableEventListener;
import com.pamirs.pradar.interceptor.Interceptors;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
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
 * druid插件
 *
 * @author vincent
 */
@MetaInfServices(ExtensionModule.class)
@ModuleInfo(id = "alibaba-druid", version = "1.0.0", author = "xiaobin@shulie.io", description = "阿里巴巴 druid 数据源")
public class AlibabaDruidPlugin extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public boolean onActive() throws Throwable {
        enhanceTemplate.enhance(this, "com.alibaba.druid.pool.DruidDataSource", new EnhanceCallback() {
            @Override
            public void doEnhance(InstrumentClass instrumentClass) {
                InstrumentMethod getConnectionMethod = instrumentClass.getDeclaredMethod("getConnection", "long");
                getConnectionMethod.addInterceptor(Listeners.of(DruidInjectGetConnectionInterceptor.class, "Druid_Get_Connection_Scope", ExecutionPolicy.BOUNDARY, Interceptors.SCOPE_CALLBACK));

                InstrumentMethod getConnectionDirect = instrumentClass.getDeclaredMethod("getConnectionDirect", "long");
                getConnectionDirect.addInterceptor(Listeners.of(DruidGetConnectionDirectInterceptor.class));
            }
        });

        ignoredTypesBuilder.ignoreClass("com.alibaba.druid.");


//        addListener();
        return true;
    }

    private void addListener() {
        EventRouter.router()
                .addListener(new DruidShadowDisableEventListener())
                .addListener(new DruidShadowActiveEventListener());
    }
}
