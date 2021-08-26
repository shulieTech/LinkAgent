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
package com.pamirs.attach.plugin.shadowjob.common.api;


public class JobAPIFactory {

    /**
     * 创建操作作业API对象.
     *
     * @param connectString 注册中心连接字符串
     * @param namespace     注册中心命名空间
     * @param digest        注册中心凭证
     * @return 操作作业API对象
     */
    public static JobOperateAPI createJobOperateAPI(final String connectString, final String namespace, final String digest) {
        return new JobOperateAPIImpl(RegistryCenterFactory.createCoordinatorRegistryCenter(connectString, namespace, digest));
    }
}
