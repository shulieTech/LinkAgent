package com.pamirs.attach.plugin.apache.kafkav2.producer.proxy;

import io.shulie.instrument.module.isolation.proxy.ShadowMethodProxy;

/**
 * @author Licey
 * @date 2022/8/2
 */
public class SendOffsetsToTransactionMethodProxy implements ShadowMethodProxy {
    @Override
    public Object executeMethod(Object shadowTarget, String method, Object... args) {
        //todo@langyi
        return null;
    }
}
