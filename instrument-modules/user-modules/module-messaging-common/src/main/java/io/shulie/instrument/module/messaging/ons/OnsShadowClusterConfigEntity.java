package io.shulie.instrument.module.messaging.ons;

import com.google.gson.Gson;
import com.pamirs.pradar.gson.GsonFactory;
import org.apache.commons.lang.StringUtils;

public class OnsShadowClusterConfigEntity {

    private String topic;
    private String groupId;
    private String namesrvAddr;

    private String shadowTopic;
    private String shadowGroupId;
    private String shadowAccessKey;
    private String shadowSecretKey;
    private String shadowNamesrvAddr;

    public void check() {
        Gson gson = GsonFactory.getGson();
        if (StringUtils.isEmpty(topic) || StringUtils.isEmpty(namesrvAddr)) {
            throw new IllegalArgumentException("[aliyun-openservices]: shadow cluster config content is not eligible, topic or namesrvAddr is empty: " + gson.toJson(this));
        }
        if (StringUtils.isEmpty(shadowAccessKey) || StringUtils.isEmpty(shadowSecretKey) || StringUtils.isEmpty(shadowNamesrvAddr)) {
            throw new IllegalArgumentException("[aliyun-openservices]: shadow cluster config content is not eligible, shadowAccessKey or shadowSecretKey or shadowNamesrvAddr is empty: " + gson.toJson(this));
        }
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public String getShadowAccessKey() {
        return shadowAccessKey;
    }

    public void setShadowAccessKey(String shadowAccessKey) {
        this.shadowAccessKey = shadowAccessKey;
    }

    public String getShadowSecretKey() {
        return shadowSecretKey;
    }

    public void setShadowSecretKey(String shadowSecretKey) {
        this.shadowSecretKey = shadowSecretKey;
    }

    public String getShadowNamesrvAddr() {
        return shadowNamesrvAddr;
    }

    public void setShadowNamesrvAddr(String shadowNamesrvAddr) {
        this.shadowNamesrvAddr = shadowNamesrvAddr;
    }

    public String getShadowTopic() {
        return shadowTopic;
    }

    public void setShadowTopic(String shadowTopic) {
        this.shadowTopic = shadowTopic;
    }

    public String getShadowGroupId() {
        return shadowGroupId;
    }

    public void setShadowGroupId(String shadowGroupId) {
        this.shadowGroupId = shadowGroupId;
    }
}
