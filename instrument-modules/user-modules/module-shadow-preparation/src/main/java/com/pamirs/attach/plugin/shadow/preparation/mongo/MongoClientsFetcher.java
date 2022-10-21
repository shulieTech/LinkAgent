package com.pamirs.attach.plugin.shadow.preparation.mongo;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;

import java.util.*;

public class MongoClientsFetcher {

    private static String mongo_client_sync_key = "com.mongodb.client.internal.MongoClientImpl";

    private static WeakHashMap<Object, String> bizClients = new WeakHashMap<>();
    private static WeakHashMap<Object, String> shadowClients = new WeakHashMap<>();

    public static synchronized void refreshClients() {
        fetchClientsForMongo(SyncObjectService.removeSyncObject(mongo_client_sync_key));
    }

    private static void fetchClientsForMongo(SyncObject syncObject) {
        if (syncObject == null) {
            return;
        }
        // 影子url集合
        Set<String> shadowKeys = buildShadowDataSourceKeys().keySet();
        // 构造函数有重载，去重
        Set<Object> processed = new HashSet<>();

        for (SyncObjectData sync : syncObject.getDatas()) {
            Object target = sync.getTarget();
            if (processed.contains(target)) {
                continue;
            }
            Object[] args = sync.getArgs();
            Object settings = null;
            for (Object arg : args) {
                if (arg.getClass().getName().equals("com.mongodb.MongoClientSettings")) {
                    settings = arg;
                    break;
                }
            }
            List collections = ReflectionUtils.getFieldValues(settings, "clusterSettings", "hosts");
            Object host = collections.get(0);
            String address = ReflectionUtils.invoke(host, "toString");
            Optional<String> shadowKey = shadowKeys.stream().filter(s -> s.contains(address)).findAny();
            if (shadowKey.isPresent()) {
                shadowClients.put(target, shadowKey.get());
            } else {
                bizClients.put(target, address);
            }
            processed.add(target);
        }
    }

    /**
     * 构建数据源key集合
     *
     * @return
     */
    private static Map<String, String> buildShadowDataSourceKeys() {
        Map<String, String> datasourceKeys = new HashMap<>();
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            datasourceKeys.put(config.getShadowUrl(), config.getUrl());
        }
        return datasourceKeys;
    }

    public static Object getBizClient(String url) {
        for (Map.Entry<Object, String> entry : bizClients.entrySet()) {
            String address = entry.getValue();
            if (url.contains(address)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Set<String> getShadowKeys() {
        return new HashSet<String>(shadowClients.values());
    }

    public static void clearShadowCache(){
        shadowClients.clear();
    }

    /**
     * 是否是mongo v4
     *
     * @return
     */
    public static boolean isMongoV4() {
        Object client = bizClients.entrySet().iterator().next().getKey();
        String dependencyPath = client.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        String[] splits = dependencyPath.split("-");
        return splits[splits.length - 1].startsWith("4");
    }
}
