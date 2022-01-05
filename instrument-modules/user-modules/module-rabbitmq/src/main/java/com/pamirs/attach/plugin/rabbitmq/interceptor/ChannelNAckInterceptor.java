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
package com.pamirs.attach.plugin.rabbitmq.interceptor;

import javax.annotation.Resource;

import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.destroy.RabbitmqDestroy;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.rabbitmq.client.Channel;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/19 4:39 下午
 */
@Destroyable(RabbitmqDestroy.class)
@ListenerBehavior(isNoSilence = true)
public class ChannelNAckInterceptor extends CutoffInterceptorAdaptor {

    private final SimulatorConfig simulatorConfig;

    @Resource
    private DynamicFieldManager manager;

    private final Logger logger = LoggerFactory.getLogger(ChannelNAckInterceptor.class);

    public ChannelNAckInterceptor(SimulatorConfig simulatorConfig) {this.simulatorConfig = simulatorConfig;}

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (Pradar.isClusterTest()) {
            Channel target = (Channel)advice.getTarget();
            Channel ptChannel = ChannelHolder.isShadowChannel(target) ? target : ChannelHolder.getShadowChannel(
                (Channel)advice.getTarget());
            if (ptChannel != null) { //如果这里==null，说明是spring的，走CutOffResult.passed();
                ptChannel.basicAck((Long)advice.getParameterArray()[0], (Boolean)advice.getParameterArray()[1]);
            }
        }
        return CutOffResult.passed();
    }
}
