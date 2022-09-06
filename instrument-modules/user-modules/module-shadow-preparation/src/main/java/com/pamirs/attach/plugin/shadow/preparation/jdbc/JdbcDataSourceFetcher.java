package com.pamirs.attach.plugin.shadow.preparation.jdbc;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadow.preparation.constants.ShadowPreparationConstants;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;

import javax.sql.DataSource;
import java.util.*;

public class JdbcDataSourceFetcher {

    private static WeakHashMap<DataSource, String> bizDataSources = new WeakHashMap<DataSource, String>();
    private static WeakHashMap<DataSource, String> shadowDataSources = new WeakHashMap<DataSource, String>();


    private static Map<String, String[]> datasourcePropertiesMappings = new HashMap<String, String[]>();

    static {
        datasourcePropertiesMappings.put(ShadowPreparationConstants.dbcp_datasource_sync_key, new String[]{"driverClassName", "url", "username", "password", "dbcp"});
        datasourcePropertiesMappings.put(ShadowPreparationConstants.dbcp2_datasource_sync_key, new String[]{"driverClassName", "url", "userName", "password", "dbcp2"});
        datasourcePropertiesMappings.put(ShadowPreparationConstants.druid_datasource_sync_key, new String[]{"driverClass", "jdbcUrl", "username", "password", "druid"});
        datasourcePropertiesMappings.put(ShadowPreparationConstants.hikaricp_datasource_sync_key, new String[]{"driverClassName", "jdbcUrl", "username", "password", "hikaricp"});
    }

    public static void refreshDataSources() {

        for (Map.Entry<String, String[]> entry : datasourcePropertiesMappings.entrySet()) {
            SyncObject syncObject = SyncObjectService.getSyncObject(entry.getKey());
            if (syncObject != null) {
                String[] properties = entry.getValue();
                fetchDataSource(syncObject.getDatas(), properties[0], properties[1], properties[2], properties[3], properties[4]);
            }
        }
        for (String key : datasourcePropertiesMappings.keySet()) {
            SyncObjectService.removeSyncObject(key);
        }

        fetchDataSourceForC3p0(SyncObjectService.getSyncObject(ShadowPreparationConstants.c3p0_datasource_sync_key));
        SyncObjectService.removeSyncObject(ShadowPreparationConstants.c3p0_datasource_sync_key);
    }

    private static void fetchDataSource(List<SyncObjectData> dataSources, String driver, String url, String userName, String password, String connectionPool) {
        Set<String> shadowKeys = buildDataSourceKeys().keySet();
        for (SyncObjectData sync : dataSources) {
            Object target = sync.getTarget();
            String jdbcUrl = ReflectionUtils.<String>get(target, url);
            String jdbcUsername = ReflectionUtils.<String>get(target, userName);
            // 剔除影子数据源
            String key = DbUrlUtils.getKey(jdbcUrl, jdbcUsername);
            if (shadowKeys.contains(key)) {
                shadowDataSources.put((DataSource) target, key);
            } else {
                bizDataSources.put((DataSource) target, key);
            }
        }
    }

    private static void fetchDataSourceForC3p0(SyncObject syncObject) {
        if (syncObject == null) {
            return;
        }
        Set<String> shadowKeys = buildDataSourceKeys().keySet();
        for (SyncObjectData sync : syncObject.getDatas()) {
            Object target = sync.getTarget();
            String url = ReflectionUtils.getFieldValues(target, "dmds", "jdbcUrl");
            Properties properties = ReflectionUtils.getFieldValues(target, "dmds", "properties");
            String username = properties.getProperty("user");
            // 剔除影子数据源
            String key = DbUrlUtils.getKey(url, username);
            if (shadowKeys.contains(key)) {
                shadowDataSources.put((DataSource) target, key);
            } else {
                bizDataSources.put((DataSource) target, key);
            }
        }
    }

    /**
     * 构建数据源key集合
     *
     * @return
     */
    private static Map<String, String> buildDataSourceKeys() {
        Map<String, String> datasourceKeys = new HashMap<String, String>();
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            datasourceKeys.put(DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername()), DbUrlUtils.getKey(config.getUrl(), config.getUsername()));
        }
        return datasourceKeys;
    }

    public static String fetchDriverClassName(DataSource dataSource) {
        String className = dataSource.getClass().getName();
        if (ShadowPreparationConstants.dbcp_datasource_sync_key.equals(className)
                || ShadowPreparationConstants.dbcp2_datasource_sync_key.equals(className)
                || ShadowPreparationConstants.hikaricp_datasource_sync_key.equals(className)) {
            return ReflectionUtils.get(dataSource, "driverClassName");
        } else if (ShadowPreparationConstants.druid_datasource_sync_key.equals(className)) {
            return ReflectionUtils.get(dataSource, "driverClass");
        } else {
            return ReflectionUtils.getFieldValues(dataSource, "dmds", "driverClass");
        }
    }

    public static DataSource getShadowDataSource(String key) {
        for (Map.Entry<DataSource, String> entry : shadowDataSources.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static DataSource getBizDataSource(String key) {
        for (Map.Entry<DataSource, String> entry : bizDataSources.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
