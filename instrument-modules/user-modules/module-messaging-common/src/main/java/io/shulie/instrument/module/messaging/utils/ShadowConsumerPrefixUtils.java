package io.shulie.instrument.module.messaging.utils;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;

public class ShadowConsumerPrefixUtils {

    /**
     * 获取影子group
     *
     * @param group
     * @return
     */
    public static String getShadowGroup(String group) {
        String shadowGroup = GlobalConfig.getInstance().getShadowGroupMappings().get(group);
        return shadowGroup != null ? shadowGroup : Pradar.addClusterTestPrefix(group);
    }

    /**
     * 获取影子topic
     *
     * @param topic
     * @return
     */
    public static String getShadowTopic(String topic) {
        String shadowTopic = GlobalConfig.getInstance().getShadowTopicMappings().get(topic);
        return shadowTopic != null ? shadowTopic : Pradar.addClusterTestPrefix(topic);
    }

}
