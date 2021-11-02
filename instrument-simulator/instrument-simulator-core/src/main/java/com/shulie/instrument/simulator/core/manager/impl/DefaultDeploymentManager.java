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

import com.shulie.instrument.simulator.api.spi.DeploymentManager;
import org.apache.commons.lang.reflect.MethodUtils;

/**
 * default implement of deployment manager
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/6/11 4:34 下午
 */
public class DefaultDeploymentManager implements DeploymentManager {
    private Class deploymentClass;

    public DefaultDeploymentManager(Class deploymentClass) {
        this.deploymentClass = deploymentClass;
    }

    @Override
    public void uninstall() throws Throwable {

        MethodUtils.invokeStaticMethod(
                deploymentClass,
                "uninstall",
                new Object[0]
        );
    }
}
