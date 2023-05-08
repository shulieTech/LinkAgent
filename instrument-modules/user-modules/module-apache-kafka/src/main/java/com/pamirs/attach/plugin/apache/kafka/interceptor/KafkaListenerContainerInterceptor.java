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
package com.pamirs.attach.plugin.apache.kafka.interceptor;

import com.pamirs.attach.plugin.apache.kafka.ConfigCache;
import com.pamirs.attach.plugin.apache.kafka.KafkaConstants;
import com.pamirs.attach.plugin.apache.kafka.destroy.KafkaDestroy;
import com.pamirs.attach.plugin.apache.kafka.origin.ConsumerHolder;
import com.pamirs.attach.plugin.apache.kafka.util.ShadowConsumerHolder;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.agent.shared.util.PradarSpringUtil;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.ModuleController;
import com.shulie.instrument.simulator.api.resource.ReleaseResource;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 创建影子Consumer
 *
 * @Author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @package: com.pamirs.attach.plugin.apache.kafka.interceptor
 * @Date 2020-04-03 16:47
 */
@Destroyable(KafkaDestroy.class)
public class KafkaListenerContainerInterceptor extends AroundInterceptor {

    private final static Logger logger = LoggerFactory.getLogger(KafkaListenerContainerInterceptor.class.getName());

    @Resource
    private ModuleController moduleController;

    @Override
    public void doBefore(Advice advice) {
        if (PradarSpringUtil.getBeanFactory() == null) {
            if (!tryRefreshBeanFactory(advice.getTarget())) {
                logger.warn("can't init shadow consumer because spring beanFactory is null, plz init spring context!");
                return;
            }
        }
        Object thisObj = advice.getTarget();
        /**
         * 如果已经初始化过了，则忽略
         */
        if (ConfigCache.isInited(thisObj)) {
            return;
        }
        Object externalContainer = null;
        try {
            externalContainer = ReflectionUtils.get(thisObj, "this$0");
        } catch (Exception e) {
            logger.warn("SIMULATOR: kafka consumer register error. can't found field this$0. {}",
                    thisObj.getClass().getName());
            return;
        }
        if (externalContainer == null) {
            logger.warn("SIMULATOR: kafka consumer register error. field this$0 is null. {}", thisObj.getClass().getName());
            return;
        }
        if (externalContainer instanceof KafkaMessageListenerContainer &&
                Pradar.isClusterTestPrefix(((KafkaMessageListenerContainer) externalContainer).getBeanName())) {
            return;
        }

        Object container = null;
        try {
            container = ReflectionUtils.get(externalContainer, KafkaConstants.REFLECT_FIELD_THIS_OR_PARENT_CONTAINER);
        } catch (Exception e) {
        }

        if (container == null) {
            try {
                container = ReflectionUtils.get(externalContainer, KafkaConstants.REFLECT_FIELD_CONTAINER);
            } catch (Exception e) {
            }
        }

        if (container == null) {
            container = externalContainer;
            logger.warn("SIMULATOR: kafka consumer register error. field {} is null. {}",
                    KafkaConstants.REFLECT_FIELD_CONTAINER, externalContainer.getClass().getName());
            //            return;
        }

        Object containerProperties = null;
        try {
            containerProperties = ReflectionUtils.get(container, KafkaConstants.REFLECT_FIELD_CONTAINER_PROPERTIES);
        } catch (Exception e) {
        }
        if (containerProperties == null) {
            try {
                containerProperties = ReflectionUtils.get(container, KafkaConstants.REFLECT_METHOD_GET_CONTAINER_PROPERTIES);
            } catch (Exception e) {
            }
        }

        if (containerProperties == null) {
            logger.warn("SIMULATOR: kafka consumer register error. got a null containerProperties from {}.",
                    container.getClass().getName());
            return;
        }

        String groupId = null;
        try {
            groupId = ReflectionUtils.invoke(containerProperties, KafkaConstants.REFLECT_METHOD_GET_GROUP_ID);
        } catch (Exception e) {
        }
        if (groupId == null) {
            groupId = fetchGroupIdFromConfig(containerProperties);
        }
        if (groupId == null) {
            logger.warn("SIMULATOR: shadow kafka consumer config cant find groupId, containerProperties : {}", containerProperties);
            return;
        }
        ReflectionUtils.invoke(containerProperties, "setGroupId", groupId);
        logger.info("SIMULATOR: shadow kafka consumer groupId: {}", groupId);
        /**
         * 如果是影子 topic，则不需要再创建对应的消费者
         */
        List<String> topicList = getShadowTopics(containerProperties, groupId);
        if (CollectionUtils.isEmpty(topicList)) {
            logger.warn("SIMULATOR: shadow kafka consumer config not found  for groupId : {} containerProperties : {}",
                    groupId, containerProperties);
            return;
        }
        Object messageListener = null;
        try {
            messageListener = ReflectionUtils.invoke(containerProperties, KafkaConstants.REFLECT_METHOD_GET_MESSAGE_LISTENER);
        } catch (Exception e) {
        }

        if (null == messageListener) {
            logger.warn("SIMULATOR: kafka consumer register error. got a null messageListener from {}.",
                    containerProperties);
            return;
        }

        Object ptObject = null;
        try {
            ptObject = ReflectionUtils.newInstance(containerProperties.getClass(), topicList.toArray(new String[0]));
        } catch (Exception e) {
        }
        if (null == ptObject) {
            logger.warn("SIMULATOR: kafka consumer register error. got a null messageListener from {}.",
                    containerProperties);
            return;
        }

        // 设置业务Consumer属性
        setContainerProperties(ptObject, containerProperties);
        try {
            ReflectionUtils.invoke(ptObject, KafkaConstants.REFLECT_METHOD_SET_MESSAGE_LISTENER, messageListener);
        } catch (Exception e) {
        }

        final DefaultListableBeanFactory defaultListableBeanFactory = PradarSpringUtil.getBeanFactory();
        // 获取bean工厂并转换为DefaultListableBeanFactory
        final String beanName = toShadowTopicString(topicList) + groupId + container.getClass().getSimpleName();
        int concurrency = 0;
        try {
            concurrency = ReflectionUtils.invoke(container, KafkaConstants.REFLECT_METHOD_GET_CONCURRENCY);
        } catch (Exception e) {
            //低版本spring-kafka无法获取到该值,TODO 考虑是否通过配置的方式
            concurrency = 1;
        }

        Object consumerFactory = null;
        try {
            consumerFactory = ReflectionUtils.get(container, KafkaConstants.REFLECT_FIELD_CONSUMER_FACTORY);
        } catch (Exception e) {
        }
        if (consumerFactory == null) {
            logger.warn("SIMULATOR: kafka consumer register error. got a null consumerFactory from {}.", container);
            return;
        }

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(
                        ConcurrentMessageListenerContainer.class)
                .setInitMethodName("doStart")
                .addConstructorArgValue(consumerFactory)
                .addConstructorArgValue(ptObject)
                .addPropertyValue("concurrency", concurrency);

        // 注册bean
        defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.getBeanDefinition());
        saveTopicGroupInfo(topicList, groupId, beanName, System.identityHashCode(thisObj));

        /**
         * 添加释放资源,会在模块卸载的时候调用
         */
        moduleController.addReleaseResource(new ReleaseResource<Object>(null) {
            @Override
            public void release() {
                Object bean = defaultListableBeanFactory.getBean(beanName);
                if (bean != null) {
                    try {
                        ReflectionUtils.invoke(bean, KafkaConstants.REFLECT_METHOD_STOP);
                    } catch (Exception e) {
                    }
                }
                defaultListableBeanFactory.removeBeanDefinition(beanName);
            }
        });
        PradarSpringUtil.getBeanFactory().getBean(beanName);
        ConsumerHolder.addShadowContainerBeanName(beanName);
        ConfigCache.setInited(advice.getTarget());
        if (logger.isInfoEnabled()) {
            logger.info("SIMULATOR: kafka consumer register successful!. groupId : {}, topic : {}",
                    Pradar.addClusterTestPrefix(groupId), topicList);
        }
    }

    private void saveTopicGroupInfo(List<String> topics, String group, String beanName, int hashcode) {
        if (topics.size() == 1) {
            ShadowConsumerHolder.topicGroupBeanNameMap.put(topics.get(0) + "#" + group, beanName);
            ShadowConsumerHolder.topicGroupCodeMap.put(topics.get(0) + "#" + group, hashcode);
            return;
        }
        StringBuilder s = new StringBuilder();
        for (String topic : topics) {
            s.append(topic).append("#");
        }
        ShadowConsumerHolder.topicGroupCodeMap.put(s.append(group).toString(), hashcode);
        ShadowConsumerHolder.topicGroupBeanNameMap.put(s.append(group).toString(), beanName);
    }

    private boolean tryRefreshBeanFactory(Object target) {
        try {
            KafkaMessageListenerContainer kafkaMessageListenerContainer = ReflectionUtils.get(target, "this$0");
            ApplicationEventPublisher applicationEventPublisher
                    = kafkaMessageListenerContainer.getApplicationEventPublisher();
            if (applicationEventPublisher instanceof AnnotationConfigApplicationContext) {
                PradarSpringUtil.refreshBeanFactory(
                        (DefaultListableBeanFactory) ((AnnotationConfigApplicationContext) applicationEventPublisher).getAutowireCapableBeanFactory());
                return true;
            } else if (applicationEventPublisher instanceof AbstractRefreshableApplicationContext) {
                PradarSpringUtil.refreshBeanFactory(
                        (DefaultListableBeanFactory) ((AbstractRefreshableApplicationContext) applicationEventPublisher).getAutowireCapableBeanFactory());
                return true;
            } else {
                try {
                    PradarSpringUtil.refreshBeanFactory(
                            (DefaultListableBeanFactory) ReflectionUtils.invoke(applicationEventPublisher, "getAutowireCapableBeanFactory"));
                    return true;
                } catch (Exception e) {
                    logger.warn(
                            "tryRefreshBeanFactory fail spring-kafka version is not support, applicationEventPublisher is a "
                                    + applicationEventPublisher.getClass().getName());
                }
            }
        } catch (Exception e) {
            logger.warn("kafka tryRefreshBeanFactory fail", e);
        }
        return false;
    }

    private String toShadowTopicString(List<String> list) {
        StringBuilder builder = new StringBuilder();
        for (String str : list) {
            builder.append(str).append('_');
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    /**
     * 设置 ContainerProperties 的属性
     *
     * @param ptContainerProperties
     * @param orgContainerProperties
     */
    private void setContainerProperties(Object ptContainerProperties, Object orgContainerProperties) {
        //不管高低版本，全部采用反射的方式
        // 设置AckTime 属性
        Object ackTime = null;
        try {
            ackTime = ReflectionUtils.invoke(orgContainerProperties, KafkaConstants.REFLECT_METHOD_GET_ACK_TIME);
        } catch (Exception e) {
        }
        if (ackTime != null) {
            long ackTimeLong = Long.valueOf(String.valueOf(ackTime));
            try {
                ReflectionUtils.invoke(ptContainerProperties, KafkaConstants.REFLECT_METHOD_SET_ACK_TIME, ackTimeLong);
            } catch (Exception e) {
            }
        }

        // 设置groupid 属性
        String groupId = null;
        try {
            groupId = ReflectionUtils.invoke(orgContainerProperties, KafkaConstants.REFLECT_METHOD_GET_GROUP_ID);
        } catch (Exception e) {
        }

        if (groupId != null && !Pradar.isClusterTestPrefix(groupId)) {
            try {
                ReflectionUtils.invoke(ptContainerProperties, KafkaConstants.REFLECT_METHOD_SET_GROUP_ID,
                        Pradar.addClusterTestPrefix(groupId));
            } catch (Exception e) {
            }
        }

        // 设置AckMode 属性
        Object ackMode = null;
        try {
            ackMode = ReflectionUtils.invoke(orgContainerProperties, KafkaConstants.REFLECT_METHOD_GET_ACK_MODE);
        } catch (Exception e) {
        }

        if (ackMode != null) {
            try {
                ReflectionUtils.invoke(ptContainerProperties, KafkaConstants.REFLECT_METHOD_SET_ACK_MODE, ackMode);
            } catch (Exception e) {
            }
        }

        // 设置AckMode 属性
        Long pollTimeout = null;
        try {
            pollTimeout = ReflectionUtils.invoke(orgContainerProperties, KafkaConstants.REFLECT_METHOD_GET_POLL_TIMEOUT);
        } catch (Exception e) {
        }

        // 设置PollTimeout 属性
        if (pollTimeout != null) {
            try {
                ReflectionUtils.invoke(ptContainerProperties, KafkaConstants.REFLECT_METHOD_SET_POLL_TIMEOUT, pollTimeout);
            } catch (Exception e) {
            }
        }
    }

    /**
     * 获取Topic
     *
     * @param object
     * @return
     */
    private List<String> getShadowTopics(Object object, String groupId) {
        List<String> topicList = new ArrayList<String>();
        String[] topics = null;
        try {
            topics = ReflectionUtils.invoke(object, KafkaConstants.REFLECT_METHOD_GET_TOPICS);
        } catch (Exception e) {
        }
        if (topics == null) {
            try {
                topics = ReflectionUtils.get(object, KafkaConstants.REFLECT_FIELD_TOPICS);
            } catch (Exception e) {
            }
        }
        if (topics != null) {
            for (String topic : topics) {
                /**
                 * topic 都需要在白名单中配置好才可以启动
                 */
                if (StringUtils.isNotBlank(topic) && !Pradar.isClusterTestPrefix(topic)) {
                    if (PradarSwitcher.whiteListSwitchOn() && GlobalConfig.getInstance().getMqWhiteList().contains(topic)
                            || GlobalConfig.getInstance().getMqWhiteList().contains(topic + '#' + groupId)) {
                        topicList.add(Pradar.addClusterTestPrefix(topic));
                    }
                }
            }
        }
        return topicList;
    }

    /**
     * 如果最终都没有取到groupId, 则从控制台配置上获取
     *
     * @param object
     * @return
     */
    private String fetchGroupIdFromConfig(Object object) {
        String[] topics = null;
        try {
            topics = ReflectionUtils.invoke(object, KafkaConstants.REFLECT_METHOD_GET_TOPICS);
        } catch (Exception e) {
        }
        if (topics == null) {
            try {
                topics = ReflectionUtils.get(object, KafkaConstants.REFLECT_FIELD_TOPICS);
            } catch (Exception e) {
            }
        }
        if (!PradarSwitcher.whiteListSwitchOn()) {
            return null;
        }
        if (topics != null) {
            Set<String> mqWhiteList = GlobalConfig.getInstance().getMqWhiteList();
            for (String topic : topics) {
                /**
                 * topic 都需要在白名单中配置好才可以启动
                 */
                if (StringUtils.isNotBlank(topic) && !Pradar.isClusterTestPrefix(topic)) {
                    for (String mq : mqWhiteList) {
                        String[] list = mq.split("#");
                        if (list.length == 2 && topic.equals(list[0])) {
                            return list[1];
                        }
                    }
                }
            }
        }
        return null;

    }

    @Override
    public void doException(Advice advice) {
        Throwable throwable = advice.getThrowable();
        logger.error(throwable.getMessage(), throwable);
        ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.MQ)
                .setErrorCode("MQ-0001")
                .setMessage("kafka-AbstractMessageListenerContainer启动失败！")
                .setDetail(throwable.getMessage())
                .report();

    }
}
