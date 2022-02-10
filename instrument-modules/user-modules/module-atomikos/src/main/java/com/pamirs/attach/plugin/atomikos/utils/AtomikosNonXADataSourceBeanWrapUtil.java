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
package com.pamirs.attach.plugin.atomikos.utils;

import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DatabaseUtils;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AtomikosNonXADataSourceBeanWrapUtil {
    private static Logger logger = LoggerFactory.getLogger(AtomikosNonXADataSourceBeanWrapUtil.class.getName());
    private static final boolean isInfoEnabled = logger.isInfoEnabled();

    public static final ConcurrentHashMap<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource> pressureDataSources = new ConcurrentHashMap<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource>();

    public static void destroy() {
        Iterator<Map.Entry<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource>> it = pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        pressureDataSources.clear();
    }

    public static boolean validate(AtomikosNonXADataSourceBean sourceDataSource) {
        try {
            String url = sourceDataSource.getUrl();
            String username = sourceDataSource.getUser();
            boolean contains = GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, username));
            if (!contains) {
                return GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, null));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }


    public static boolean shadowTable(AtomikosNonXADataSourceBean sourceDataSource) {
        try {
            String url = sourceDataSource.getUrl();
            String username = sourceDataSource.getUser();
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
    private static boolean isPerformanceDataSource(AtomikosNonXADataSourceBean target) {
        for (Map.Entry<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource> entry : pressureDataSources.entrySet()) {
            AtomikosNonXADataSourceBeanMediaDataSource mediatorDataSource = entry.getValue();
            if (mediatorDataSource.getDataSourcePerformanceTest() == null) {
                continue;
            }
            if (StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getUrl(), target.getUrl())
                    && StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getUser(), target.getUser())) {
                return true;
            }
        }
        return false;
    }

    //  static AtomicBoolean inited = new AtomicBoolean(false);

    public static void init(DataSourceMeta<AtomikosNonXADataSourceBean> dataSourceMeta) {
        if (pressureDataSources.containsKey(dataSourceMeta) && pressureDataSources.get(dataSourceMeta) != null) {
            return;
        }
        AtomikosNonXADataSourceBean target = dataSourceMeta.getDataSource();
        if (isPerformanceDataSource(target)) {
            return;
        }
        if (!validate(target)) {
            //没有配置对应的影子表或影子库
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0002")
                    .setMessage("没有配置对应的影子表或影子库！")
                    .setDetail("[atomikos] DataSourceWrapUtil:业务库配置::url: " + target.getUrl()  + "username：" + dataSourceMeta.getUsername() + "; 中间件类型：other")
                    .report();

            AtomikosNonXADataSourceBeanMediaDataSource dbMediatorDataSource = new AtomikosNonXADataSourceBeanMediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);

            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                if (isInfoEnabled) {
                    logger.info("[atomikos] destroyed shadow table datasource success. url:{} ,username:{}", target.getUrl(), target.getUser());
                }
                old.close();
            }
            return;
        }
        if (shadowTable(target)) {
            //影子表
            try {
                AtomikosNonXADataSourceBeanMediaDataSource dbMediatorDataSource = new AtomikosNonXADataSourceBeanMediaDataSource();
                dbMediatorDataSource.setDataSourceBusiness(target);

                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
                if (old != null) {
                    if (isInfoEnabled) {
                        logger.info("[atomikos] destroyed shadow table datasource success. url:{} ,username:{}", target.getUrl(), target.getUser());
                    }
                    old.close();
                }
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子表设置初始化异常！")
                        .setDetail("[atomikos] DataSourceWrapUtil:业务库配置:::url: " + target.getUrl() + "|||" + Throwables.getStackTraceAsString(e))
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
                logger.error("[atomikos] init datasource err!", e);
            }
        } else {
            //影子库
            try {
                AtomikosNonXADataSourceBeanMediaDataSource dataSource = new AtomikosNonXADataSourceBeanMediaDataSource();
                AtomikosNonXADataSourceBean ptDataSource = copy(target);
                dataSource.setDataSourcePerformanceTest(ptDataSource);
                dataSource.setDataSourceBusiness(target);

                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dataSource);
                if (old != null) {
                    if (isInfoEnabled) {
                        logger.info("[atomikos] destroyed shadow table datasource success. url:{} ,username:{}", target.getUrl(), target.getUser());
                    }
                    old.close();
                }
                if (isInfoEnabled) {
                    logger.info("[atomikos] create shadow datasource success. target:{} url:{} ,username:{} shadow-url:{},shadow-username:{}", target.hashCode(), target.getUrl(), target.getUser(), ptDataSource.getUrl(), ptDataSource.getUser());
                }
            } catch (Throwable t) {
                logger.error("[atomikos] init datasource err!", t);
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子库设置初始化异常！")
                        .setDetail("[atomikos] DataSourceWrapUtil:业务库配置:::url: " + target.getUrl() + "|||" + Throwables.getStackTraceAsString(t))
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
            }
        }
    }

    private static AtomikosNonXADataSourceBean copy(AtomikosNonXADataSourceBean source) {
        AtomikosNonXADataSourceBean target = generate(source);
        return target;
    }


    public static AtomikosNonXADataSourceBean generate(AtomikosNonXADataSourceBean sourceDatasource) {
        Map<String, ShadowDatabaseConfig> conf = GlobalConfig.getInstance().getShadowDatasourceConfigs();
        if (conf == null) {
            return null;
        }
        ShadowDatabaseConfig ptDataSourceConf
                = selectMatchPtDataSourceConfiguration(sourceDatasource, conf);
        if (ptDataSourceConf == null) {
            return null;
        }
        String url = ptDataSourceConf.getShadowUrl();
        String username = ptDataSourceConf.getShadowUsername();
        String password = ptDataSourceConf.getShadowPassword();
        String driverClassName = ptDataSourceConf.getShadowDriverClassName();
        if (StringUtils.isBlank(driverClassName)) {
            driverClassName = sourceDatasource.getDriverClassName();
        }


        if (StringUtils.isBlank(url) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            logger.error("ATOMIKOS: url/username/password can't not be null,{} {} {}", url, username, password);
            return null;
        }
        AtomikosNonXADataSourceBean target = new AtomikosNonXADataSourceBean();
        target.setUrl(url);
        target.setUser(username);
        target.setPassword(password);
        target.setDriverClassName(driverClassName);

        Integer minPoolSize = ptDataSourceConf.getIntProperty("minPoolSize");
        if (minPoolSize != null) {
            target.setMinPoolSize(minPoolSize);
        } else {
            target.setMinPoolSize(sourceDatasource.getMinPoolSize());
        }

        Integer maxPoolSize = ptDataSourceConf.getIntProperty("maxPoolSize");
        if (maxPoolSize != null) {
            target.setMaxPoolSize(maxPoolSize);
        } else {
            target.setMaxPoolSize(sourceDatasource.getMaxPoolSize());
        }

        Integer poolSize = ptDataSourceConf.getIntProperty("poolSize");
        if (poolSize != null) {
            target.setPoolSize(poolSize);
        }

        Integer borrowConnectionTimeout = ptDataSourceConf.getIntProperty("borrowConnectionTimeout");
        if (borrowConnectionTimeout != null) {
            target.setBorrowConnectionTimeout(borrowConnectionTimeout);
        } else {
            target.setBorrowConnectionTimeout(sourceDatasource.getBorrowConnectionTimeout());
        }

        Integer reapTimeout = ptDataSourceConf.getIntProperty("reapTimeout");
        if (reapTimeout != null) {
            target.setReapTimeout(reapTimeout);
        } else {
            target.setReapTimeout(sourceDatasource.getReapTimeout());
        }

        Integer maintenanceInterval = ptDataSourceConf.getIntProperty("maintenanceInterval");
        if (maintenanceInterval != null) {
            target.setMaintenanceInterval(Integer.valueOf(maintenanceInterval));
        } else {
            target.setMaintenanceInterval(sourceDatasource.getMaintenanceInterval());
        }

        Integer maxIdleTime = ptDataSourceConf.getIntProperty("maxIdleTime");
        if (maxIdleTime != null) {
            target.setMaxIdleTime(Integer.valueOf(maxIdleTime));
        } else {
            target.setMaxIdleTime(sourceDatasource.getMaxIdleTime());
        }

        Integer maxLifetime = ptDataSourceConf.getIntProperty("maxLifetime");
        if (maxLifetime != null) {
            target.setMaxLifetime(Integer.valueOf(maxLifetime));
        } else {
            target.setMaxLifetime(sourceDatasource.getMaxLifetime());
        }

        String testQuery = ptDataSourceConf.getProperty("testQuery");
        if (StringUtils.isNotBlank(testQuery)) {
            target.setTestQuery(testQuery);
        }

        Boolean concurrentConnectionValidation = ptDataSourceConf.getBooleanProperty("concurrentConnectionValidation");
        if (concurrentConnectionValidation != null) {
            target.setConcurrentConnectionValidation(Boolean.valueOf(concurrentConnectionValidation));
        }

        String resourceName = ptDataSourceConf.getProperty("resourceName");
        if (StringUtils.isNotBlank(resourceName)) {
            target.setUniqueResourceName(resourceName);
        } else {
            target.setUniqueResourceName(Pradar.addClusterTestPrefix(sourceDatasource.getUniqueResourceName()));
        }

        Integer defaultIsolationLevel = ptDataSourceConf.getIntProperty("defaultIsolationLevel");
        if (defaultIsolationLevel != null) {
            target.setDefaultIsolationLevel(Integer.valueOf(defaultIsolationLevel));
        }

        return target;
    }

    private static ShadowDatabaseConfig selectMatchPtDataSourceConfiguration(AtomikosNonXADataSourceBean source, Map<String, ShadowDatabaseConfig> shadowDbConfigurations) {
        AtomikosNonXADataSourceBean dataSource = source;
        String key = DbUrlUtils.getKey(dataSource.getUrl(), dataSource.getUser());
        ShadowDatabaseConfig shadowDatabaseConfig = shadowDbConfigurations.get(key);
        if (shadowDatabaseConfig == null) {
            key = DbUrlUtils.getKey(dataSource.getUrl(), null);
            shadowDatabaseConfig = shadowDbConfigurations.get(key);
        }
        return shadowDatabaseConfig;
    }

}
