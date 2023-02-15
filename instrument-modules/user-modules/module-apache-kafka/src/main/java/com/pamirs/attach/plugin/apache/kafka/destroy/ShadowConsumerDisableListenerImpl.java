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
package com.pamirs.attach.plugin.apache.kafka.destroy;

import com.pamirs.attach.plugin.apache.kafka.ConfigCache;
import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerMetaData;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerProxy;
import com.pamirs.attach.plugin.apache.kafka.util.ShadowConsumerHolder;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerDisableEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.listener.ShadowConsumerDisableListener;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerDisableInfo;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.reflect.ReflectException;
import io.shulie.instrument.module.messaging.utils.ShadowConsumerPrefixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author angju
 * @date 2021/10/11 10:50
 */
public class ShadowConsumerDisableListenerImpl implements ShadowConsumerDisableListener, PradarEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowConsumerDisableListenerImpl.class);


    @Override
    public boolean disableBatch(List<ShadowConsumerDisableInfo> list) {
        boolean result = true;
        for (ShadowConsumerDisableInfo shadowConsumerDisableInfo : list) {
            String topic = shadowConsumerDisableInfo.getTopic(), group = shadowConsumerDisableInfo.getConsumerGroup();
            String springKey = ShadowConsumerPrefixUtils.getShadowTopic(topic, group) +
                    "#" + ShadowConsumerPrefixUtils.getShadowGroup(topic, group);
            String originKey = topic + "#" + group;
            if (ShadowConsumerHolder.topicGroupBeanNameMap.containsKey(springKey)) {
                disableBatchSpringKafka(springKey);
                break;
            } else if (ConsumerHolder.getShadowProxyMapping().containsKey(originKey)) {
                disableBatchOriginKafka(originKey, shadowConsumerDisableInfo.getTopic(), shadowConsumerDisableInfo.getConsumerGroup());
                break;
            }

        }
        return result;
    }

    private void disableBatchOriginKafka(String key, String topic, String group) {
        try {
            int code = ConsumerHolder.getShadowProxyMapping().get(key);
            ConsumerProxy consumerProxy = ConsumerHolder.getProxyMapping().get(code);
            consumerProxy.closePtConsumer();
            ConsumerHolder.getProxyMapping().remove(code);
            ConsumerHolder.getCache().remove(code);
            for (Map.Entry<Integer, ConsumerMetaData> entry : ConsumerHolder.getCache().entrySet()) {
                if (entry.getValue().getTopics().contains(ShadowConsumerPrefixUtils.getShadowTopic(topic,group)) &&
                        entry.getValue().getGroupId().equals(ShadowConsumerPrefixUtils.getShadowGroup(topic,group))) {
                    ConsumerHolder.getCache().remove(entry.getKey());
                    break;
                }
            }
            ConsumerHolder.getShadowProxyMapping().remove(key);
        } catch (Throwable t) {
            logger.error("[apache-kafka]: {}", Throwables.getStackTraceAsString(t));
        }
    }

    private void disableBatchSpringKafka(String key) {
        String beanName = ShadowConsumerHolder.topicGroupBeanNameMap.get(key);
        remove(beanName);
        ShadowConsumerHolder.topicGroupBeanNameMap.remove(key);
        ConfigCache.getIsInited().remove(ShadowConsumerHolder.topicGroupCodeMap.get(key));
    }

    private void disableAllSpringKafka() {
        for (String beanName : ShadowConsumerHolder.topicGroupBeanNameMap.values()) {
            remove(beanName);
        }
        ShadowConsumerHolder.topicGroupBeanNameMap.clear();
        ConfigCache.getIsInited().clear();
    }

    private void disableAllOriginKafka() {
        for (ConsumerProxy consumerProxy : ConsumerHolder.getProxyMapping().values()) {
            consumerProxy.closePtConsumer();
        }
        ConsumerHolder.getProxyMapping().clear();
        ConsumerHolder.getCache().clear();
        ConsumerHolder.getShadowProxyMapping().clear();
    }

    @Override
    public boolean disableAll() {
        disableAllSpringKafka();
        disableAllOriginKafka();
        return true;
    }

    Logger logger = LoggerFactory.getLogger(getClass());

    private void remove(String beanName) {
        try {
            Object bean = PradarSpringUtil.getBeanFactory().getBean(beanName);
            if (bean != null) {
                try {
                    if (bean instanceof org.springframework.context.Lifecycle) {
                        try {
                            ((org.springframework.context.Lifecycle) bean).stop();
                        } catch (Throwable t) {
                            ReflectionUtils.invoke(bean, KafkaConstants.REFLECT_METHOD_STOP);
                        }
                    }
                } catch (ReflectException e) {
                }
            }
            PradarSpringUtil.getBeanFactory().removeBeanDefinition(beanName);
        } catch (Throwable t) {
            logger.error("[apache-kafka]:{}", Throwables.getStackTraceAsString(t));
        }
    }


    @Override
    public EventResult onEvent(IEvent event) {
        try {
            if (event instanceof ShadowConsumerDisableEvent) {
                ShadowConsumerDisableEvent shadowConsumerDisableEvent = (ShadowConsumerDisableEvent) event;
                List<ShadowConsumerDisableInfo> list = shadowConsumerDisableEvent.getTarget();
                disableBatch(list);
            } else if (event instanceof ClusterTestSwitchOffEvent) {
                disableAll();
            } else if (event instanceof SilenceSwitchOnEvent) {
                disableAll();
            }
        } catch (Throwable e) {
            EventResult.error(event.getTarget(), e.getMessage());
        }

        return EventResult.success(event.getTarget());
    }

    @Override
    public int order() {
        return 98;
    }
}
