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
package com.pamirs.attach.plugin.dynamic;

import com.shulie.instrument.simulator.api.ExtensionModule;
import com.shulie.instrument.simulator.api.ModuleInfo;
import com.shulie.instrument.simulator.api.ModuleLifecycleAdapter;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.kohsuke.MetaInfServices;

import javax.annotation.Resource;

/**
 * @Auther: vernon
 * @Date: 2021/8/17 21:41
 * @Description:
 */

@ModuleInfo(id = "dynamic-resource-common", version = "1.0.0", author = "vernon", description = "动态资源抽象模块")
@MetaInfServices(ExtensionModule.class)
public class DynamicResourceModule extends ModuleLifecycleAdapter implements ExtensionModule {

    @Override
    public void onActive() throws Throwable {
        super.onActive();
    }


}
