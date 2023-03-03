package io.shulie.instrument.module.messaging.utils;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerDisableEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerEnableEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShadowConsumerPrefixUtils {

    private static Map<String, String> shadowTopicMappings = new ConcurrentHashMap<>();
    private static Map<String, String> shadowGroupMappings = new ConcurrentHashMap<>();

    static {
        EventRouter.router().addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (event instanceof ShadowConsumerDisableEvent || event instanceof ShadowConsumerEnableEvent) {
                    refreshMappingsCaches();
                }
                return EventResult.IGNORE;
            }

            @Override
            public int order() {
                return 33;
            }
        });
    }

    /**
     * 获取影子topic
     *
     * @param topic
     * @return
     */
    public static String getShadowTopic(String topic, String group) {
        String topicGroup = topic + "#" + group;
        String shadowTopic = shadowTopicMappings.get(topicGroup);
        if (shadowTopic != null) {
            return shadowTopic;
        }
        String shadowTopicGroup = GlobalConfig.getInstance().getShadowTopicGroupMappings().get(topicGroup);
        shadowTopic = shadowTopicGroup != null ? shadowTopicGroup.split("#")[0] : Pradar.addClusterTestPrefix(topic);
        shadowTopicMappings.putIfAbsent(topicGroup, shadowTopic);
        return shadowTopic;
    }

    /**
     * 获取影子group
     *
     * @param group
     * @return
     */
    public static String getShadowGroup(String topic, String group) {
        if (Pradar.isClusterTestPrefix(topic)) {
            topic = topic.substring(3);
        }
        String topicGroup = topic + "#" + group;
        String shadowGroup = shadowGroupMappings.get(topicGroup);
        if (shadowGroup != null) {
            return shadowGroup;
        }
        String shadowTopicGroup = GlobalConfig.getInstance().getShadowTopicGroupMappings().get(topic + "#" + group);
        shadowGroup = shadowTopicGroup != null ? shadowTopicGroup.split("#")[1] : Pradar.addClusterTestPrefix(group);
        shadowGroupMappings.putIfAbsent(topicGroup, shadowGroup);
        return shadowGroup;
    }

    private static void refreshMappingsCaches() {
        shadowTopicMappings.clear();
        shadowGroupMappings.clear();
    }


}
