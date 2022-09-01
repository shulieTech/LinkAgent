package com.pamirs.attach.plugin.shadow.preparation.jdbc;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadow.preparation.constants.ShadowPreparationConstants;
import com.pamirs.attach.plugin.shadow.preparation.entity.JdbcDataSourceEntity;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;

import java.util.*;

public class JdbcBizDataSourceFetcher {

    private static Set<JdbcDataSourceEntity> bizDataSources = new HashSet<JdbcDataSourceEntity>();

    private static Map<String, String[]> datasourcePropertiesMappings = new HashMap<String, String[]>();

    static {
        datasourcePropertiesMappings.put(ShadowPreparationConstants.dbcp_datasource_sync_key, new String[]{"driverClassName", "url", "username", "password", "dbcp"});
        datasourcePropertiesMappings.put(ShadowPreparationConstants.dbcp2_datasource_sync_key, new String[]{"driverClassName", "url", "userName", "password", "dbcp2"});
        datasourcePropertiesMappings.put(ShadowPreparationConstants.druid_datasource_sync_key, new String[]{"driverClass", "jdbcUrl", "username", "password", "druid"});
        datasourcePropertiesMappings.put(ShadowPreparationConstants.hikaricp_datasource_sync_key, new String[]{"driverClassName", "jdbcUrl", "username", "password", "hikaricp"});
    }

    public static void fetchBizDataSource() {
        for (Map.Entry<String, String[]> entry : datasourcePropertiesMappings.entrySet()) {
            SyncObject syncObject = SyncObjectService.getSyncObject(entry.getKey());
            if (syncObject != null) {
                String[] properties = entry.getValue();
                buildBizDataSource(syncObject.getDatas(), properties[0], properties[1], properties[2], properties[3], properties[4]);
            }
        }
        buildBizDataSourceForC3p0(SyncObjectService.getSyncObject(ShadowPreparationConstants.c3p0_datasource_sync_key));
    }

    private static void buildBizDataSource(List<SyncObjectData> dataSources, String driver, String url, String userName, String password, String connectionPool) {
        Set<String> shadowKeys = buildShadowConfigKeys();
        for (SyncObjectData sync : dataSources) {
            Object target = sync.getTarget();
            JdbcDataSourceEntity entity = new JdbcDataSourceEntity();
            entity.setDriverClassName(ReflectionUtils.<String>get(target, driver));
            entity.setUrl(ReflectionUtils.<String>get(target, url));
            entity.setUserName(ReflectionUtils.<String>get(target, userName));
            entity.setPassword("********");
            entity.setConnectionPool(connectionPool);
            // 剔除影子数据源
            String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
            if (!shadowKeys.contains(key)) {
                bizDataSources.add(entity);
            }
        }
    }

    private static void buildBizDataSourceForC3p0(SyncObject syncObject) {
        if (syncObject == null) {
            return;
        }
        Set<String> shadowKeys = buildShadowConfigKeys();
        for (SyncObjectData sync : syncObject.getDatas()) {
            Object target = sync.getTarget();
            JdbcDataSourceEntity entity = new JdbcDataSourceEntity();
            entity.setDriverClassName((String) ReflectionUtils.getFieldValues(target, "dmds", "driverClass"));
            entity.setUrl((String) ReflectionUtils.getFieldValues(target, "dmds", "jdbcUrl"));

            Properties properties = ReflectionUtils.getFieldValues(target, "dmds", "properties");
            entity.setUserName(properties.getProperty("user"));
            entity.setPassword("********");
            entity.setConnectionPool("c3p0");
            // 剔除影子数据源
            String key = DbUrlUtils.getKey(entity.getUrl(), entity.getUserName());
            if (!shadowKeys.contains(key)) {
                bizDataSources.add(entity);
            }
        }
    }

    private static Set<String> buildShadowConfigKeys() {
        Set<String> shadowKeys = new HashSet<String>();
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            shadowKeys.add(DbUrlUtils.getKey(config.getShadowUrl(), config.getShadowUsername()));
        }
        return shadowKeys;
    }


}
