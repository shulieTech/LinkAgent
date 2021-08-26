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
package com.mongodb.client.internal;

import com.mongodb.ReadPreference;
import com.mongodb.internal.binding.ConnectionSource;
import com.mongodb.internal.binding.ReadWriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.internal.binding.ClusterAwareReadWriteBinding;
import com.mongodb.internal.session.SessionContext;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/27 6:25 下午
 */
public class ClientSessionBinding implements ReadWriteBinding {
    public ClientSessionBinding(final ClientSession session, final boolean ownsSession, final ClusterAwareReadWriteBinding wrapped) {
    }

    public ClientSessionBinding(final ClientSession session, final boolean ownsSession, final ReadWriteBinding wrapped) {
    }

    @Override
    public ReadPreference getReadPreference() {
        return null;
    }

    @Override
    public ConnectionSource getReadConnectionSource() {
        return null;
    }

    @Override
    public ConnectionSource getWriteConnectionSource() {
        return null;
    }

    @Override
    public SessionContext getSessionContext() {
        return null;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public ReadWriteBinding retain() {
        return null;
    }

    @Override
    public void release() {

    }
}
