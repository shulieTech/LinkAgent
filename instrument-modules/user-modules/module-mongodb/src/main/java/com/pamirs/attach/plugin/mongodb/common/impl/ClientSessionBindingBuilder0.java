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
