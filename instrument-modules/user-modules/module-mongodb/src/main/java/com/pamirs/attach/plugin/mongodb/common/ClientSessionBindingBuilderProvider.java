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
package com.pamirs.attach.plugin.mongodb.common;

import com.mongodb.binding.ReadWriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.client.internal.ClientSessionBinding;
import com.mongodb.internal.binding.ClusterAwareReadWriteBinding;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/27 6:27 下午
 */
public class ClientSessionBindingBuilderProvider {

    public static ReadWriteBinding build(Class clazz, ClientSession session, boolean ownsSession,
        ReadWriteBinding readWriteBinding) {
        if (isVersion0(clazz)) {
            return new ClientSessionBinding(session, ownsSession, readWriteBinding);
        }
        if (isVersion1(clazz)) {
            return new ClientSessionBinding(session, ownsSession, (ClusterAwareReadWriteBinding)readWriteBinding);
        }
        return null;
    }

    public static boolean isVersion1(Class clazz) {
        try {
            return clazz.getDeclaredConstructor(ClientSession.class, boolean.class, ClusterAwareReadWriteBinding.class)
                != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean isVersion0(Class clazz) {
        try {
            return clazz.getDeclaredConstructor(ClientSession.class, boolean.class, ReadWriteBinding.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
