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

package com.pamirs.attach.plugin.apache.kafkav2.producer.proxy;

import com.pamirs.pradar.Pradar;
import io.shulie.instrument.module.isolation.proxy.impl.ModifyParamShadowMethodProxy;
import kafka.producer.KeyedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/8 11:46
 */
public class JavaApiProducerSendProxy extends ModifyParamShadowMethodProxy {

    private static final Logger logger = LoggerFactory.getLogger(JavaApiProducerSendProxy.class);

    @Override
    public Object[] fetchParam(Object shadowTarget, Method method, Object... args) {
        if (args.length < 1) {
            return args;
        }
        try {
            final Object obj = args[0];
            if (obj == null) {
                return args;
            }
            if (obj instanceof KeyedMessage) {
                args[0] = copyBizMsg((KeyedMessage) obj);
            }

            if (obj instanceof List && !((List<?>) obj).isEmpty()) {
                List<KeyedMessage> bizMsgList = (List<KeyedMessage>) obj;
                List<KeyedMessage> shadowMsgList = new ArrayList<>(bizMsgList.size());
                for (KeyedMessage item : bizMsgList) {
                    shadowMsgList.add(copyBizMsg(item));
                }
                args[0] = shadowMsgList;
            }
        } catch (Throwable e) {
            logger.warn("SIMULATOR: origin kafka send message failed.", e);
        }
        return args;
    }


    /**
     * 拷贝业务的 KeyedMessage对象
     *
     * @param bizMsg 业务KeyedMessage对象
     * @return 影子KeyedMessage对象
     */
    private KeyedMessage copyBizMsg(KeyedMessage bizMsg) {
        return new KeyedMessage(Pradar.addClusterTestPrefix(bizMsg.topic()), bizMsg.key(), bizMsg.partKey(), bizMsg.message());
    }
}
