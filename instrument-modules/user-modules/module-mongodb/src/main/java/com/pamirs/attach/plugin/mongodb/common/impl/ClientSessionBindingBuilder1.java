package com.pamirs.attach.plugin.mongodb.common.impl;

import com.mongodb.binding.ReadWriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.client.internal.ClientSessionBinding;
import com.mongodb.internal.binding.ClusterAwareReadWriteBinding;
import com.pamirs.attach.plugin.mongodb.common.ReadWriteBindingBuilder;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/9/27 6:20 下午
 */
public class ClientSessionBindingBuilder1 implements ReadWriteBindingBuilder {
    @Override
    public ReadWriteBinding build(ClientSession session, boolean ownsSession, ReadWriteBinding readWriteBinding) {
        return new ClientSessionBinding(session, ownsSession, (ClusterAwareReadWriteBinding) readWriteBinding);
    }

    @Override
    public boolean isSupported(Class clazz) {
        try {
            return clazz.getDeclaredConstructor(ClientSession.class, boolean.class, ClusterAwareReadWriteBinding.class) != null;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
