package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.pressurement.agent.event.impl.MqWhiteListConfigEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerDisableEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerEnableEvent;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerDisableInfo;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerEnableInfo;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import io.shulie.agent.management.client.constant.ConfigResultEnum;
import io.shulie.agent.management.client.model.Config;
import io.shulie.agent.management.client.model.ConfigAck;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

public class MqConfigPushCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MqConfigPushCommandProcessor.class.getName());

    public static void processConfigPushCommand(final Config config, final Consumer<ConfigAck> callback) {
        LOGGER.info("[shadow-preparation] accept shadow mq push command, content:{}", config.getParam());

        ConfigAck ack = new ConfigAck();
        ack.setType(config.getType());
        ack.setVersion(config.getVersion());
        ack.setResultCode(ConfigResultEnum.SUCC.getCode());

        JSONArray mapList = JSON.parseArray(config.getParam());
        Set<String> mqList = new HashSet<>();

        for (int i = 0; i < mapList.size(); i++) {
            JSONObject stringObjectMap = (JSONObject) mapList.get(i);
            Map<String, List<String>> topicGroups = (Map<String, List<String>>) stringObjectMap.get("topicGroups");
            Set<Map.Entry<String, List<String>>> entries = topicGroups.entrySet();

            for (Map.Entry<String, List<String>> entry : entries) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                for (int j = 0; j < values.size(); j++) {
                    String value = key + "#" + values.get(j);
                    mqList.add(value);
                }
            }
        }
        compareIsChangeAndSet(mqList);

        callback.accept(ack);

    }

    private static void compareIsChangeAndSet(Set<String> newValue) {
        final MqWhiteListConfigEvent mqWhiteListConfigEvent = new MqWhiteListConfigEvent(newValue);
        EventRouter.router().publish(mqWhiteListConfigEvent);
        Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
        if (compareEquals(mqWhiteList.size(), newValue.size())
                && mqWhiteList.containsAll(newValue)) {
            return;
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

        PradarSwitcher.turnConfigSwitcherOn(ConfigNames.MQ_WHITE_LIST);
        GlobalConfig.getInstance().setMqWhiteList(newValue);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[shadow-preparation] publish mq whitelist config successful. config={}", newValue);
        }
    }

    private static boolean compareEquals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.equals(object2);
    }

}
