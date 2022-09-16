/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.pulsar.interceptor;

import com.pamirs.attach.plugin.pulsar.cache.ProducerCache;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.ProducerImpl;
import org.apache.pulsar.client.impl.SendCallback;
import org.apache.pulsar.client.impl.conf.ProducerConfigurationData;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/9/6 14:38
 */
public class PulsarProducerInterceptor extends CutoffInterceptorAdaptor {

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }

        ProducerImpl bizProducer = (ProducerImpl) advice.getTarget();

        if (Pradar.isClusterTestPrefix(bizProducer.getTopic())) {
            return CutOffResult.passed();
        }

        Producer shadowProducer = ProducerCache.findShadowProducer(bizProducer, new ProducerCache.Supplier() {
            @Override
            public Producer get(ProducerImpl bizProducer) throws PulsarClientException {
                PulsarClient client = bizProducer.getClient();

                ProducerConfigurationData configurationData = bizProducer.getConfiguration();

                Schema schema = Reflect.on(bizProducer).get("schema");
                Producer shadowProducer = client.newProducer(schema)
                        .topic(Pradar.addClusterTestPrefix(bizProducer.getTopic()))
                        .create();

                Reflect.on(shadowProducer).set("conf", configurationData);
                return shadowProducer;
            }
        });

        ((ProducerImpl) shadowProducer).sendAsync((Message) advice.getParameterArray()[0],
                (SendCallback) advice.getParameterArray()[1]);

        return CutOffResult.cutoff(null);

    }
}
