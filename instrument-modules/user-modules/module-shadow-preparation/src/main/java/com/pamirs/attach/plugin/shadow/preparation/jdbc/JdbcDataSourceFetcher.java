package com.pamirs.attach.plugin.shadow.preparation.jdbc;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.constants.JdbcDataSourceClassPropertiesEnum;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;

import javax.sql.DataSource;
import java.util.*;

public class JdbcDataSourceFetcher {

    private static String c3p0_datasource_sync_key = "com.mchange.v2.c3p0.ComboPooledDataSource";

    private static WeakHashMap<DataSource, String> bizDataSources = new WeakHashMap<DataSource, String>();
    private static WeakHashMap<DataSource, String> shadowDataSources = new WeakHashMap<DataSource, String>();

    public static synchronized void refreshDataSources() {
        for (JdbcDataSourceClassPropertiesEnum value : JdbcDataSourceClassPropertiesEnum.getValues()) {
            SyncObject syncObject = SyncObjectService.removeSyncObject(value.getClassName());
            if (syncObject != null) {
                fetchDataSource(syncObject.getDatas(), value.getJdbcUrlProperty(), value.getUsernameProperty());
            }
        }
        fetchDataSourceForC3p0(SyncObjectService.removeSyncObject(c3p0_datasource_sync_key));
    }

    private static void fetchDataSource(List<SyncObjectData> dataSources, String url, String userName) {
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

    /**
     * 取驱动class名称
     *
     * @param dataSource
     * @return
     */
    public static String fetchDriverClassName(DataSource dataSource) {
        String className = dataSource.getClass().getName();
        JdbcDataSourceClassPropertiesEnum properties = JdbcDataSourceClassPropertiesEnum.getEnumByClassName(className);
        if (properties != null) {
            return ReflectionUtils.get(dataSource, properties.getDriverClassProperty());
        } else if (c3p0_datasource_sync_key.equals(className)) {
            return ReflectionUtils.getFieldValues(dataSource, "dmds", "driverClass");
        }
        return null;
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

    public static Set<String> getShadowKeys() {
        return new HashSet<String>(shadowDataSources.values());
    }

    public static int getShadowDataSourceNum() {
        return shadowDataSources.size();
    }

    public static void removeShadowDataSources(Collection<String> keys) {
        Iterator<Map.Entry<DataSource, String>> iterator = shadowDataSources.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DataSource, String> next = iterator.next();
            if (keys.contains(next.getValue())) {
                iterator.remove();
            }
        }
    }

}
