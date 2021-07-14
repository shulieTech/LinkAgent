package com.mongodb.client.internal;

import com.mongodb.ReadPreference;
import com.mongodb.binding.ConnectionSource;
import com.mongodb.binding.ReadWriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.internal.binding.ClusterAwareReadWriteBinding;
import com.mongodb.session.SessionContext;

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
