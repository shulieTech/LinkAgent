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
package com.pamirs.attach.plugin.mongodb.common.impl;

import com.mongodb.binding.ReadWriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.client.internal.ClientSessionBinding;
import com.pamirs.attach.plugin.mongodb.common.ReadWriteBindingBuilder;

/**
 * 低版本的ClientSessionBinding构建器
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/27 6:20 下午
 */
public class ClientSessionBindingBuilder0 implements ReadWriteBindingBuilder {
    @Override
    public ReadWriteBinding build(ClientSession session, boolean ownsSession, ReadWriteBinding readWriteBinding) {
        return new ClientSessionBinding(session, ownsSession, readWriteBinding);
    }

    @Override
    public boolean isSupported(Class clazz) {
        try {
            return clazz.getDeclaredConstructor(ClientSession.class, boolean.class, ReadWriteBinding.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
