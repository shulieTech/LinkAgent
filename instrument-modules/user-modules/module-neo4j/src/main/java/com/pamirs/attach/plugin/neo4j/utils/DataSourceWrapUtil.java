/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.neo4j.utils;

import com.pamirs.attach.plugin.neo4j.config.DriverConfig;
import com.pamirs.attach.plugin.neo4j.config.Neo4JSessionExt;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.service.DriverService;
import org.neo4j.ogm.session.Neo4jSession;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: DataSourceWrapUtil
 * @author: wangjian
 * @Date: 2020/8/19 17:42
 * @Description:
 */
public class DataSourceWrapUtil {
    public static final ConcurrentHashMap<DataSourceMeta, DbMediatorDataSource<Neo4jSession>> pressureDataSources
            = new ConcurrentHashMap<DataSourceMeta, DbMediatorDataSource<Neo4jSession>>();
    public static Map<Neo4jSession, MetaData> metaDataMap = new HashMap<Neo4jSession, MetaData>();
    private final static Object lock = new Object();

    public static void wrap(DataSourceMeta<Neo4jSession> neo4jSessionDataSourceMeta, String pwd) {
        if (DataSourceWrapUtil.pressureDataSources.containsKey(neo4jSessionDataSourceMeta)) {
            return;
        }
        synchronized (lock) {
            if (DataSourceWrapUtil.pressureDataSources.containsKey(neo4jSessionDataSourceMeta)) {
                return;
            }
            // 生成影子库配置
            // 初始化影子库配置
            DriverConfiguration sourceConfig = Components.driver().getConfiguration();
            Configuration shadowConfig = new Configuration();
            DriverConfiguration configuration = new DriverConfiguration(shadowConfig);
            configuration.setDriverClassName(sourceConfig.getDriverClassName());
            configuration.setConnectionPoolSize(sourceConfig.getConnectionPoolSize());
            configuration.setEncryptionLevel(sourceConfig.getEncryptionLevel());
            configuration.setTrustCertFile(sourceConfig.getTrustCertFile());
            configuration.setTrustStrategy(sourceConfig.getTrustStrategy());
            String key = DbUrlUtils.getKey(neo4jSessionDataSourceMeta.getUrl(), neo4jSessionDataSourceMeta.getUsername());
            ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
            if (null == shadowDatabaseConfig) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0002")
                        .setMessage("没有配置对应的影子表或影子库！")
                        .setDetail("业务库配置:::url: " + neo4jSessionDataSourceMeta.getUrl())
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
                return;
            }
            configuration.setURI(shadowDatabaseConfig.getShadowUrl());
            configuration.setCredentials(shadowDatabaseConfig.getShadowUsername(neo4jSessionDataSourceMeta.getUsername()),
                    shadowDatabaseConfig.getShadowPassword(pwd));
            Driver shadowDriver = DriverService.load(configuration);
            // 生成影子库session
            Neo4jSession sourceSession = neo4jSessionDataSourceMeta.getDataSource();
            Neo4JSessionExt shadowSession = new Neo4JSessionExt(metaDataMap.get(sourceSession), shadowDriver);
            // 放入本地
            DbMediatorDataSource<Neo4jSession> driverConfigurationDbMediatorDataSource = new DriverConfig();
            driverConfigurationDbMediatorDataSource.setDataSourceBusiness(sourceSession);
            driverConfigurationDbMediatorDataSource.setDataSourcePerformanceTest(shadowSession);
            DataSourceWrapUtil.pressureDataSources.put(neo4jSessionDataSourceMeta, driverConfigurationDbMediatorDataSource);
        }
    }

    public static void destroy() {
        Iterator<Map.Entry<DataSourceMeta, DbMediatorDataSource<Neo4jSession>>> it = pressureDataSources.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, DbMediatorDataSource<Neo4jSession>> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        pressureDataSources.clear();
    }
}
