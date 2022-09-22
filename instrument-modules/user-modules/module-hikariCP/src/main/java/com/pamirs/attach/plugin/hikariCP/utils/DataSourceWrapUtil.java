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
package com.pamirs.attach.plugin.hikariCP.utils;

import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DatabaseUtils;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Auther: vernon
 * @Date: 2020/3/29 14:59
 * @Description:
 */
public class DataSourceWrapUtil {
    private static Logger logger = LoggerFactory.getLogger(DataSourceWrapUtil.class.getName());

    public static final ConcurrentHashMap<DataSourceMeta, HikariMediaDataSource> pressureDataSources = new ConcurrentHashMap<DataSourceMeta, HikariMediaDataSource>();

    public static void destroy() {
        Iterator<Map.Entry<DataSourceMeta, HikariMediaDataSource>> it = pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, HikariMediaDataSource> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        pressureDataSources.clear();
    }

    public static boolean validate(HikariDataSource sourceDataSource) {
        try {
            String url = sourceDataSource.getJdbcUrl();
            String username = sourceDataSource.getUsername();
            boolean contains = GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, username));
            if (!contains) {
                return GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, null));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean shadowTable(HikariDataSource sourceDataSource) {
        try {
            String url = sourceDataSource.getJdbcUrl();
            String username = sourceDataSource.getUsername();
            return DatabaseUtils.isTestTable(url, username);
        } catch (Throwable e) {
            return true;
        }
    }

    /**
     * 是否是影子数据源
     *
     * @param target 目标数据源
     * @return
     */
    private static boolean isPerformanceDataSource(HikariDataSource target) {
        for (Map.Entry<DataSourceMeta, HikariMediaDataSource> entry : pressureDataSources.entrySet()) {
            HikariMediaDataSource mediatorDataSource = entry.getValue();
            if (mediatorDataSource.getDataSourcePerformanceTest() == null) {
                continue;
            }
            if (StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getJdbcUrl(), target.getJdbcUrl()) &&
                    StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getUsername(), target.getUsername())) {
                logger.info("module-hikariCP isPerformanceDataSource !!!");
                return true;
            }
        }
        return false;
    }

    //  static AtomicBoolean inited = new AtomicBoolean(false);

    public static void init(DataSourceMeta<HikariDataSource> dataSourceMeta) {
        if (pressureDataSources.get(dataSourceMeta) != null) {
            return;
        }
        HikariDataSource target = dataSourceMeta.getDataSource();
        if (isPerformanceDataSource(target)) {
            return;
        }
        if (!validate(target)) {
            //没有配置对应的影子表或影子库
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0002")
                    .setMessage("没有配置对应的影子表或影子库！")
                    .setDetail("业务库配置:::url: " + target.getJdbcUrl()  + "; username：" + dataSourceMeta.getUsername() + "; 中间件类型：hikari")
                    .report();
            HikariMediaDataSource dbMediatorDataSource = new HikariMediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);
            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                if (logger.isInfoEnabled()) {
                    logger.info("[hikariCP] destroyed shadow table datasource success. url:{} ,username:{}", target.getJdbcUrl(), target.getUsername());
                }
                old.close();
            }
            return;
        }
        if (shadowTable(target)) {
            //影子表
            try {
                HikariMediaDataSource dbMediatorDataSource = new HikariMediaDataSource();
                dbMediatorDataSource.setDataSourceBusiness(target);
                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
                if (old != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("[hikariCP] destroyed shadow table datasource success. url:{} ,username:{}", target.getJdbcUrl(), target.getUsername());
                    }
                    old.close();
                }
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0002")
                        .setMessage("没有配置对应的影子表或影子库！")
                        .setDetail("业务库配置:::url: " + target.getJdbcUrl() + "; username：" + dataSourceMeta.getUsername() + "; 中间件类型：hikari" + Throwables.getStackTraceAsString(e))
                        .report();
                logger.error("[hikariCP] init datasource err!", e);
            }
        } else {
            //影子库
            try {
                HikariMediaDataSource dataSource = new HikariMediaDataSource();
                HikariDataSource ptDataSource = copy(target);
                dataSource.setDataSourcePerformanceTest(ptDataSource);
                dataSource.setDataSourceBusiness(target);
                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dataSource);
                if (old != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("[hikariCP] destroyed shadow table datasource success. url:{} ,username:{}", target.getJdbcUrl(), target.getUsername());
                    }
                    old.close();
                }
                if (logger.isInfoEnabled()) {
                    logger.info("[hikariCP] create shadow datasource success. target:{} url:{} ,username:{} shadow-url:{},shadow-username:{}", target.hashCode(), target.getJdbcUrl(), target.getUsername(), ptDataSource.getJdbcUrl(), ptDataSource.getUsername());
                }
            } catch (Throwable t) {
                logger.error("[hikariCP] init datasource err!", t);
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                        .setDetail("url: " + target.getJdbcUrl() + Throwables.getStackTraceAsString(t))
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
            }
        }
    }

    private static HikariDataSource copy(HikariDataSource source) {
        HikariDataSource target = generate(source);
        return target;
    }


    public static HikariDataSource generate(HikariDataSource sourceDatasource) {
        Map<String, ShadowDatabaseConfig> conf = GlobalConfig.getInstance().getShadowDatasourceConfigs();
        if (conf == null) {
            return null;
        }
        ShadowDatabaseConfig ptDataSourceConf
                = selectMatchPtDataSourceConfiguration(sourceDatasource, conf);
        if (ptDataSourceConf == null) {
            return null;
        }
       return generate(sourceDatasource, ptDataSourceConf);
    }

    public static HikariDataSource generate(HikariDataSource sourceDatasource, ShadowDatabaseConfig ptDataSourceConf) {
        String url = ptDataSourceConf.getShadowUrl();
        String username = ptDataSourceConf.getShadowUsername(sourceDatasource.getUsername());
        String password = ptDataSourceConf.getShadowPassword(sourceDatasource.getPassword());

        if (StringUtils.isBlank(url) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }

        String driverClassName = ptDataSourceConf.getShadowDriverClassName();
        if (StringUtils.isBlank(driverClassName)) {
            driverClassName = sourceDatasource.getDriverClassName();
        }

        HikariDataSource target = new HikariDataSource();
        target.setJdbcUrl(url);
        target.setUsername(username);
        target.setPassword(password);
        target.setDriverClassName(driverClassName);
        try {

            Integer minIdle = ptDataSourceConf.getIntProperty("minIdle");
            if (minIdle != null) {
                target.setMinimumIdle(minIdle);
            } else {
                target.setMinimumIdle(sourceDatasource.getMinimumIdle());
            }

            Long connectionTimeout = ptDataSourceConf.getLongProperty("connectionTimeout");
            if (connectionTimeout != null) {
                target.setConnectionTimeout(connectionTimeout);
            } else {
                target.setConnectionTimeout(sourceDatasource.getConnectionTimeout());
            }

            Long idleTimeout = ptDataSourceConf.getLongProperty("idleTimeout");
            if (idleTimeout != null) {
                target.setIdleTimeout(idleTimeout);
            } else {
                target.setIdleTimeout(sourceDatasource.getIdleTimeout());
            }

            Long leakDetectionThreshold = ptDataSourceConf.getLongProperty("leakDetectionThreshold");
            if (leakDetectionThreshold != null) {
                target.setLeakDetectionThreshold(leakDetectionThreshold);
            } else {
                target.setLeakDetectionThreshold(sourceDatasource.getLeakDetectionThreshold());
            }

            Long maxLifetime = ptDataSourceConf.getLongProperty("maxLifetime");
            if (maxLifetime != null) {
                target.setMaxLifetime(maxLifetime);
            } else {
                target.setMaxLifetime(sourceDatasource.getMaxLifetime());
            }

            Integer maxActive = ptDataSourceConf.getIntProperty("maxActive");
            if (maxActive != null) {
                target.setMaximumPoolSize(maxActive);
            } else {
                target.setMaximumPoolSize(sourceDatasource.getMaximumPoolSize());
            }

            Long validationTimeout = ptDataSourceConf.getLongProperty("validationTimeout");
            if (validationTimeout != null) {
                target.setValidationTimeout(validationTimeout);
            } else {
                target.setValidationTimeout(sourceDatasource.getValidationTimeout());
            }

            Integer loginTimeout = ptDataSourceConf.getIntProperty("loginTimeout");
            if (loginTimeout != null) {
                target.setLoginTimeout(loginTimeout);
            } else {
                target.setLoginTimeout(sourceDatasource.getLoginTimeout());
            }

            String connectionInitSql = ptDataSourceConf.getProperty("connectionInitSql");
            if (connectionInitSql != null) {
                target.setConnectionInitSql(connectionInitSql);
            } else {
                target.setConnectionInitSql(sourceDatasource.getConnectionInitSql());
            }

            Long maxWait = ptDataSourceConf.getLongProperty("maxWait");
            if (maxWait != null) {
                target.setConnectionTimeout(maxWait);
            } else {
                target.setConnectionTimeout(sourceDatasource.getConnectionTimeout());
            }

            String validationQuery = ptDataSourceConf.getProperty("validationQuery");
            if (validationQuery != null) {
                target.setConnectionTestQuery(validationQuery);
            } else {
                target.setConnectionTestQuery(sourceDatasource.getConnectionTestQuery());
            }

            target.setReadOnly(sourceDatasource.isReadOnly());

        } catch (Throwable e) {
            logger.warn("", e);
        }
        return target;
    }

    @SuppressWarnings("unchecked")
    private static ShadowDatabaseConfig selectMatchPtDataSourceConfiguration(HikariDataSource source, Map<String, ShadowDatabaseConfig> shadowDbConfigurations) {
        HikariDataSource dataSource = source;
        String key = DbUrlUtils.getKey(dataSource.getJdbcUrl(), dataSource.getUsername());
        ShadowDatabaseConfig shadowDatabaseConfig = shadowDbConfigurations.get(key);
        if (shadowDatabaseConfig == null) {
            key = DbUrlUtils.getKey(dataSource.getJdbcUrl(), null);
            shadowDatabaseConfig = shadowDbConfigurations.get(key);
        }
        return shadowDatabaseConfig;
    }

    private static Map<String, Object> selectDataSource(String dataSourceId, List<Map<String, Object>> dataSources) {
        for (Map<String, Object> dataSource : dataSources) {
            if (!dataSource.containsKey("id")) {
                continue;
            }
            if (dataSourceId.equals(String.valueOf(dataSource.get("id")))) {
                return dataSource;
            }
        }
        return null;
    }
}
