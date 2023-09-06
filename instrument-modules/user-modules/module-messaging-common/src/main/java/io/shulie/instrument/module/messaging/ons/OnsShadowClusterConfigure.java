package io.shulie.instrument.module.messaging.ons;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.pamirs.pradar.common.HttpUtils;
import com.pamirs.pradar.gson.GsonFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnsShadowClusterConfigure {

    static final Logger logger = LoggerFactory.getLogger(OnsShadowClusterConfigure.class);

    private static String ONS_CONFIG_URL = "%s/api/link/kafka/cluster/configs/pull?appName=%s";

    public static Map<String, OnsShadowClusterConfigEntity> ons_shadow_cluster_config = new HashMap<>();

    static {
        initOnsShadowClusterConfig(getRemoteKafkaConfig());
    }

    public static String getRemoteKafkaConfig() {
        String config;
        String troControlWebUrl = System.getProperty("tro.web.url");
        String projectName = System.getProperty("simulator.app.name");
        ONS_CONFIG_URL = String.format(ONS_CONFIG_URL, troControlWebUrl, projectName);
        HttpUtils.HttpResult httpResult = HttpUtils.doGet(ONS_CONFIG_URL);
        if (!httpResult.isSuccess()) {
            return null;
        }
        String json = httpResult.getResult();
        JsonObject jsonObject = GsonFactory.getGson().fromJson(json, JsonObject.class);
        JsonArray data = jsonObject.get("data").getAsJsonArray();
        List<JsonObject> result = new ArrayList<JsonObject>();
        if (data == null) {
            config = StringUtils.EMPTY;
        } else {
            for (int i = 0; i < data.size(); i++) {
                JsonObject item = data.get(i).getAsJsonObject();
                result.add(GsonFactory.getGson().fromJson(item.get("config").getAsString(), JsonObject.class));
            }
            config = GsonFactory.getGson().toJson(result);
        }
        return config;
    }

    private static void initOnsShadowClusterConfig(String shadowOns) {
        if (!shadowOns.contains("namesrvAddr")) {
            return;
        }
        try {
            //group-> topic ->map
            List<OnsShadowClusterConfigEntity> configBeanList = GsonFactory.getGson().fromJson(shadowOns, new TypeToken<List<OnsShadowClusterConfigEntity>>() {
            }.getType());
            for (OnsShadowClusterConfigEntity configBean : configBeanList) {
                try {
                    configBean.check();
                    ons_shadow_cluster_config.put(configBean.getTopic(), configBean);
                } catch (Throwable e) {
                    logger.warn("[aliyun-openservices] singleton config parser error", e);
                }
            }
        } catch (Throwable ex) {
            logger.warn("[aliyun-openservices] config json parser error", ex);
        }

    }


    public static OnsShadowClusterConfigEntity findShadowClusterConfig(String topic, String groupId) {
        OnsShadowClusterConfigEntity entity = OnsShadowClusterConfigure.ons_shadow_cluster_config.get(topic);
        if (entity == null) {
            return null;
        }
        if (entity.getGroupId() == null) {
            logger.error("[aliyun-openservices]: find shadow cluster config for topic:{}, but don`t have groupId!", topic);
            return null;
        }
        if (!groupId.equals(entity.getGroupId())) {
            logger.error("[aliyun-openservices]: find shadow cluster config for topic:{}, but has different groupId:{},{}", topic, groupId, entity.getGroupId());
            return null;
        }
        return entity;
    }

}
