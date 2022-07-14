/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shulie.instrument.module.config.fetcher.config.event.model;

import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.pressurement.agent.event.impl.MqWhiteListConfigEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerDisableEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerEnableEvent;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerDisableInfo;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerEnableInfo;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.module.config.fetcher.config.impl.ApplicationConfig;
import com.shulie.instrument.module.config.fetcher.config.utils.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @ClassName: RabbitWhiteList
 * @author: wangjian
 * @Date: 2020/9/29 09:54
 * @Description:
 */
public class MQWhiteList implements IChange<Set<String>, ApplicationConfig> {
    private final static Logger LOGGER = LoggerFactory.getLogger(MQWhiteList.class);
    private static MQWhiteList INSTANCE;

    public static MQWhiteList getInstance() {
        if (INSTANCE == null) {
            synchronized (MQWhiteList.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MQWhiteList();
                }
            }
        }
        return INSTANCE;
    }

    public static void release() {
        INSTANCE = null;
    }

    @Override
    public Boolean compareIsChangeAndSet(ApplicationConfig currentValue, Set<String> newValue) {
        final MqWhiteListConfigEvent mqWhiteListConfigEvent = new MqWhiteListConfigEvent(newValue);
        EventRouter.router().publish(mqWhiteListConfigEvent);
        Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
        if (ObjectUtils.equals(mqWhiteList.size(), newValue.size())
                && mqWhiteList.containsAll(newValue)) {
            return Boolean.FALSE;
        }
        // 仅对影子消费者禁用事件处理
        for (String s : mqWhiteList) {
            List<ShadowConsumerDisableInfo> disableInfos = new ArrayList<ShadowConsumerDisableInfo>();
            if (!newValue.contains(s)) {
                ShadowConsumerDisableInfo disableInfo = new ShadowConsumerDisableInfo();
                if (s.contains("@")) {//rabbitmq routing使用方式，配置为direct-exchange#queue1@queue1
                    disableInfo.setTopic(s.split("@")[1]);
                } else if (s.contains("#")) {
                    String[] topicGroup = s.split("#", 2);
                    if (StringUtils.isBlank(topicGroup[0])) {
                        disableInfo.setTopic(topicGroup[1]);
                    } else {
                        disableInfo.setTopic(topicGroup[0]);
                        disableInfo.setConsumerGroup(topicGroup[1]);
                    }
                }
                disableInfos.add(disableInfo);
            }

            if (!disableInfos.isEmpty()) {
                EventRouter.router().publish(new ShadowConsumerDisableEvent(disableInfos));
            }
        }

        // 消费者启用也发出一个事件
        for (String s : newValue) {
            List<ShadowConsumerEnableInfo> enableList = new ArrayList<ShadowConsumerEnableInfo>();
            if (!mqWhiteList.contains(s)) {
                ShadowConsumerEnableInfo enableInfo = new ShadowConsumerEnableInfo();
                if (s.contains("@")) {
                    //rabbitmq routing使用方式，配置为direct-exchange#queue1@queue1
                    enableInfo.setTopic(s.split("@")[1]);
                } else if (s.contains("#")) {
                    String[] topicGroup = s.split("#", 2);
                    if (StringUtils.isBlank(topicGroup[0])) {
                        enableInfo.setTopic(topicGroup[1]);
                    } else {
                        enableInfo.setTopic(topicGroup[0]);
                        enableInfo.setConsumerGroup(topicGroup[1]);
                    }
                }
                enableList.add(enableInfo);
            }

            if (!enableList.isEmpty()) {
                EventRouter.router().publish(new ShadowConsumerEnableEvent(enableList));
            }
        }

        currentValue.setMqList(newValue);
        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.MQ_WHITE_LIST);
        GlobalConfig.getInstance().setMqWhiteList(newValue);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("publish mq whitelist config successful. config={}", newValue);
        }
        return Boolean.TRUE;
    }
}
