/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.pradar.pressurement.agent.shared.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.StringUtils;

/**
 * @author xiaobin.zfb | xiaobin@shulie.io
 * @since 2020/9/10 12:31 下午
 */
public class ShadowDatabaseConfigParser {

    public static final String PT_PRESSURE_TABLE_PREFIX_ENV_CONFIG = "pradar.shadow.table.prefix";

    private static ShadowDatabaseConfigParser INSTANCE;

    public static ShadowDatabaseConfigParser getInstance() {
        if (INSTANCE == null) {
            synchronized (ShadowDatabaseConfigParser.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ShadowDatabaseConfigParser();
                }
            }
        }
        return INSTANCE;
    }

    public List<ShadowDatabaseConfig> parse(List<Map<String, Object>> list) {
        if (list == null || list.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<ShadowDatabaseConfig> configs = new ArrayList<ShadowDatabaseConfig>();
        for (Map<String, Object> map : list) {
            ShadowDatabaseConfig shadowDatabaseConfig = parse(map);
            if (shadowDatabaseConfig != null) {
                configs.add(shadowDatabaseConfig);
            }
        }

        return configs;
    }

    private String toString(Object object) {
        if (object == null || "null".equals(object)) {
            return null;
        }
        return StringUtils.trim(object.toString());
    }

    public ShadowDatabaseConfig parse(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        int dsType = Integer.parseInt(map.get("dsType") == null ? "-1" : map.get("dsType").toString());
        final String shadowTableConfig = StringUtils.trim(toString(map.get("shadowTableConfig")));
        Map<String, Object> shadowDbConfig = (Map<String, Object>)map.get("shadowDbConfig");

        /**
         * 这块判断一下，如果是影子库则需要校验 shadowDbConfig 不为空
         * 如果是影子表则需要校验 shadowTableConfig 不为空
         */
        if (dsType == 0) {
            if (shadowDbConfig == null) {
                return null;
            }
            /**
             * 禁用影子库+影子表模式
             */
        } else if (dsType == 1) {
            if (shadowTableConfig == null) {
                return null;
            }
        } else if (dsType == 2) {
            /**
             * 影子库中使用影子表
             */
            if (shadowDbConfig == null) {
                return null;
            }
        } else {
            return null;
        }

        ShadowDatabaseConfig shadowDatabaseConfig = new ShadowDatabaseConfig();
        shadowDatabaseConfig.setShadowAccountPrefix(
            GlobalConfig.getInstance().getSimulatorDynamicConfig().shadowDatasourceAccountPrefix());
        shadowDatabaseConfig.setShadowAccountSuffix(GlobalConfig.getInstance().getSimulatorDynamicConfig()
            .shadowDatasourceAccountSuffix());

        final String applicationName = StringUtils.trim(toString(map.get("applicationName")));
        shadowDatabaseConfig.setApplicationName(applicationName);

        shadowDatabaseConfig.setDsType(dsType);

        final String url = StringUtils.trim(toString(map.get("url")));
        shadowDatabaseConfig.setUrl(url);

        if (StringUtils.isNotBlank(shadowTableConfig)) {
            String[] arr = StringUtils.split(shadowTableConfig, ',');
            if (arr != null && arr.length != 0) {
                Map<String, String> businessTables = new ConcurrentHashMap<String, String>();
                for (String str : arr) {
                    if (StringUtils.isNotBlank(str)) {
                        /**
                         * 现在配置不支持影子表名的自定义，由系统自动生成，后面如果控制台支持后，修改会非常容易
                         */
//                        businessTables.put(StringUtils.trim(str), Pradar.addClusterTestPrefix(StringUtils.trim(str)));
                        businessTables.put(StringUtils.trim(str), getShadowTablePrefix() + StringUtils.trim(str));
                    }
                }
                shadowDatabaseConfig.setBusinessShadowTables(businessTables);
            }
        }

        if (shadowDbConfig == null) {
            return shadowDatabaseConfig;
        }
        Map<String, Object> datasourceMediator = (Map<String, Object>)shadowDbConfig.get("datasourceMediator");
        if (datasourceMediator == null) {
            return shadowDatabaseConfig;
        }

        String businessDataSourceName = null, shadowDataSourceName = null;
        businessDataSourceName = toString(datasourceMediator.get("dataSourceBusiness"));
        shadowDataSourceName = toString(datasourceMediator.get("dataSourcePerformanceTest"));

        if (businessDataSourceName == null || shadowDataSourceName == null) {
            return shadowDatabaseConfig;
        }

        List<Map<String, Object>> list = (List<Map<String, Object>>)shadowDbConfig.get("dataSources");
        if (list == null || list.isEmpty()) {
            return shadowDatabaseConfig;
        }

        Map<String, Object> businessMap = null, shadowMap = null;
        for (Map<String, Object> m : list) {
            String id = toString(m.get("id"));
            if (StringUtils.equals(id, businessDataSourceName)) {
                businessMap = m;
            } else if (StringUtils.equals(id, shadowDataSourceName)) {
                shadowMap = m;
            }
        }

        if (businessMap == null || shadowMap == null) {
            return shadowDatabaseConfig;
        }

        for (Map.Entry<String, Object> entry : businessMap.entrySet()) {
            if (StringUtils.equals(entry.getKey(), "id")) {
                continue;
            }
            if (StringUtils.equals(entry.getKey(), "url")) {
                shadowDatabaseConfig.setUrl(toString(entry.getValue()));
            } else if (StringUtils.equals(entry.getKey(), "username")) {
                shadowDatabaseConfig.setUsername(toString(entry.getValue()));
            } else if (StringUtils.equals(entry.getKey(), "schema")) {
                shadowDatabaseConfig.setSchema(toString(entry.getValue()));
            }
        }

        Map<String, String> properties = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : shadowMap.entrySet()) {
            if (StringUtils.equals(entry.getKey(), "id")) {
                continue;
            }
            if (StringUtils.equals(entry.getKey(), "url")) {
                shadowDatabaseConfig.setShadowUrl(toString(entry.getValue()));
                if (shadowDatabaseConfig.getShadowUrl().startsWith("mongodb://")) {
                    String[] s = shadowDatabaseConfig.getShadowUrl().split("/");
                    shadowDatabaseConfig.setShadowSchema(s[s.length - 1]);
                }
            } else if (StringUtils.equals(entry.getKey(), "username")) {
                shadowDatabaseConfig.setShadowUsername(toString(entry.getValue()));
            } else if (StringUtils.equals(entry.getKey(), "password")) {
                shadowDatabaseConfig.setShadowPassword(toString(entry.getValue()));
            } else if (StringUtils.equals(entry.getKey(), "driverClassName")) {
                shadowDatabaseConfig.setShadowDriverClassName(toString(entry.getValue()));
            } else if (StringUtils.equals(entry.getKey(), "schema")) {
                shadowDatabaseConfig.setShadowSchema(toString(entry.getValue()));
            } else if (StringUtils.equals(entry.getKey(), "extra")) {
                Map<String, Object> extra = (Map)entry.getValue();
                if (extra == null) {
                    continue;
                }
                for (Map.Entry<String, Object> inner : extra.entrySet()) {
                    String str = toString(inner.getValue());
                    if (StringUtils.isBlank(str)) {
                        continue;
                    }
                    properties.put(inner.getKey(), str);
                }
            } else {
                String str = toString(entry.getValue());
                if (StringUtils.isBlank(str)) {
                    continue;
                }
                properties.put(entry.getKey(), str);
            }
        }

        shadowDatabaseConfig.setProperties(properties);
        String bizschema = shadowDatabaseConfig.getSchema();
        String shadowSchema = shadowDatabaseConfig.getShadowSchema();
        if (StringUtil.isEmpty(bizschema) && !StringUtil.isEmpty(shadowSchema)) {
            shadowDatabaseConfig.setSchema(shadowSchema);
        }
        return shadowDatabaseConfig;
    }

    private String getShadowTablePrefix() {
        String pressureTablePrefix = System.getProperty(PT_PRESSURE_TABLE_PREFIX_ENV_CONFIG);
        if (StringUtils.isBlank(pressureTablePrefix)){
            pressureTablePrefix = System.getenv(PT_PRESSURE_TABLE_PREFIX_ENV_CONFIG);
        }
        if (StringUtils.isNotBlank(pressureTablePrefix)){
            return pressureTablePrefix;
        }
        return Pradar.CLUSTER_TEST_PREFIX;
    }
}
