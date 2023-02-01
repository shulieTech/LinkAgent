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

import java.util.*;

/**
 * @ClassName: RabbitWhiteList
 * @author: wangjian
 * @Date: 2020/9/29 09:54
 * @Description:
 */
public class MQWhiteList implements IChange<Set<String>, ApplicationConfig> {
    private final static Logger LOGGER = LoggerFactory.getLogger(MQWhiteList.class);
    private static MQWhiteList INSTANCE;

    private static final String shadow_flag = ">>";

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

        Set<String> mqWhiteList = restoreMqWhitelist(GlobalConfig.getInstance().getMqWhiteList());
        if (ObjectUtils.equals(mqWhiteList.size(), newValue.size()) && mqWhiteList.containsAll(newValue)) {
            return Boolean.FALSE;
        }

        // 仅对影子消费者禁用事件处理
        for (String s : mqWhiteList) {
            List<ShadowConsumerDisableInfo> disableList = new ArrayList<ShadowConsumerDisableInfo>();
            if (!newValue.contains(s)) {
                ShadowConsumerDisableInfo disableInfo = buildShadowInfo(s);
                disableList.add(disableInfo);
            }
            if (!disableList.isEmpty()) {
                EventRouter.router().publish(new ShadowConsumerDisableEvent(disableList));
            }
        }

        // 消费者启用也发出一个事件
        for (String s : newValue) {
            List<ShadowConsumerEnableInfo> enableList = new ArrayList<ShadowConsumerEnableInfo>();
            if (!mqWhiteList.contains(s)) {
                ShadowConsumerEnableInfo enableInfo = buildShadowInfo(s);
                enableList.add(enableInfo);
            }
            if (!enableList.isEmpty()) {
                EventRouter.router().publish(new ShadowConsumerEnableEvent(enableList));
            }
        }

        currentValue.setMqList(newValue);
        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.MQ_WHITE_LIST);

        Object[] objects = extractShadowConsumerInfos(newValue);
        GlobalConfig.getInstance().setMqWhiteList((Set<String>) objects[0]);
        GlobalConfig.getInstance().setShadowTopicMappings((Map<String, String>) objects[1]);
        GlobalConfig.getInstance().setShadowGroupMappings((Map<String, String>) objects[2]);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("publish mq whitelist config successful. config={}", newValue);
        }
        return Boolean.TRUE;
    }

    private ShadowConsumerDisableInfo buildShadowInfo(String topicGroup) {
        ShadowConsumerDisableInfo enableInfo = new ShadowConsumerDisableInfo();
        if (topicGroup.contains("@")) {
            //rabbitmq routing使用方式，配置为direct-exchange#queue1@queue1
            enableInfo.setTopic(topicGroup.split("@")[1]);
        } else if (topicGroup.contains("#")) {
            String[] topicGroups = topicGroup.split("#", 2);
            String topic = topicGroups[0], group = topicGroups[1];
            if (StringUtils.isBlank(topic)) {
                enableInfo.setTopic(extractBizGroup(group));
            } else {
                enableInfo.setTopic(extractBizTopic(topic));
                enableInfo.setConsumerGroup(extractBizGroup(group));
            }
        }
        return enableInfo;
    }

    /**
     * 把mqWhitelist恢复成从控制台拉取到的数据格式, bizTopic>>shadowTopic#bizGroup>>shadowGroup
     *
     * @param mqWhiteList
     * @return
     */
    private Set<String> restoreMqWhitelist(Set<String> mqWhiteList) {
        Set<String> rawWhitelist = new HashSet<String>();
        Map<String, String> shadowTopicMappings = GlobalConfig.getInstance().getShadowTopicMappings();
        Map<String, String> shadowGroupMappings = GlobalConfig.getInstance().getShadowGroupMappings();

        for (String s : mqWhiteList) {
            if (!s.contains("#")) {
                rawWhitelist.add(s);
                continue;
            }
            String[] split = s.split("#", 2);
            String topic = split[0], group = split[1];
            if (shadowTopicMappings.containsKey(topic)) {
                topic = topic + shadow_flag + shadowTopicMappings.get(topic);
            }
            if (shadowGroupMappings.containsKey(group)) {
                group = group + shadow_flag + shadowGroupMappings.get(group);
            }
            rawWhitelist.add(topic + "#" + group);
        }
        return rawWhitelist;
    }

    /**
     * 提取自定义的影子消费者信息
     *
     * @param mqWhiteList
     * @return
     */
    private Object[] extractShadowConsumerInfos(Set<String> mqWhiteList) {
        Set<String> topicGroups = new HashSet<String>();
        Map<String, String> shadowGroupMappings = new HashMap<String, String>(), shadowTopicMappings = new HashMap<String, String>();
        for (String s : mqWhiteList) {
            if (!s.contains("#")) {
                topicGroups.add(s);
                continue;
            }
            String[] split = s.split("#", 2);
            String topic = split[0], group = split[1];
            if (topic.contains(shadow_flag)) {
                String[] topics = topic.split(shadow_flag, 2);
                shadowTopicMappings.put(topics[0], topics[1]);
                topic = topics[0];
            }
            if (group.contains(shadow_flag)) {
                String[] groups = group.split(shadow_flag, 2);
                shadowGroupMappings.put(groups[0], groups[1]);
                group = groups[0];
            }
            topicGroups.add(topic + "#" + group);
        }
        return new Object[]{topicGroups, shadowTopicMappings, shadowGroupMappings};
    }

    private String extractBizTopic(String topic) {
        return extractBizElement(topic);
    }

    private String extractBizGroup(String Group) {
        return extractBizElement(Group);
    }

    private String extractBizElement(String topicOrGroup) {
        return topicOrGroup.contains(shadow_flag) ? topicOrGroup.split(">>")[0] : topicOrGroup;
    }
}
