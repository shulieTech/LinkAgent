package io.shulie.instrument.module.messaging.utils;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;

public class ShadowConsumerPrefixUtils {

    /**
     * 获取影子topic
     *
     * @param topic
     * @return
     */
    public static String getShadowTopic(String topic, String group) {
        String shadowTopicGroup = GlobalConfig.getInstance().getShadowTopicGroupMappings().get(topic + "#" + group);
        return shadowTopicGroup != null ? shadowTopicGroup.split("#")[0] : Pradar.addClusterTestPrefix(topic);
    }

    /**
     * 获取影子group
     *
     * @param group
     * @return
     */
    public static String getShadowGroup(String topic, String group) {
        String shadowTopicGroup = GlobalConfig.getInstance().getShadowTopicGroupMappings().get(topic + "#" + group);
        return shadowTopicGroup != null ? shadowTopicGroup.split("#")[1] : Pradar.addClusterTestPrefix(group);
    }


}
