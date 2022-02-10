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

import com.atomikos.jdbc.AtomikosDataSourceBean;
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
import com.shulie.druid.util.JdbcUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class AtomikosDataSourceBeanWrapUtil {
    private static Logger logger = LoggerFactory.getLogger(AtomikosDataSourceBeanWrapUtil.class.getName());
    private final static boolean isInfoEnabled = logger.isInfoEnabled();

    public static final ConcurrentHashMap<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource> pressureDataSources = new ConcurrentHashMap<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource>();

    public static void destroy() {
        Iterator<Map.Entry<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource>> it = pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        pressureDataSources.clear();
    }

    public static String getUsername(AtomikosDataSourceBean datasource) {
        String username = datasource.getXaProperties().getProperty("user");
        if (username == null) {
            username = datasource.getXaProperties().getProperty("username");
        }
        if (username == null) {
            username = datasource.getXaProperties().getProperty("User");
        }
        return username;
    }

    public static String getUrl(AtomikosDataSourceBean datasource) {
        String url = datasource.getXaProperties().getProperty("URL");
        if (url == null) {
            url = datasource.getXaProperties().getProperty("url");
        }
        return url;
    }

    public static String getDriverClassName(AtomikosDataSourceBean datasource) {
        String driverClassName = datasource.getXaProperties().getProperty("driverClassName");
        if (driverClassName == null) {
            driverClassName = datasource.getXaProperties().getProperty("driverClass");
        }
        if (driverClassName == null) {
            driverClassName = datasource.getXaProperties().getProperty("driver");
        }
        return driverClassName;
    }

    public static String getProperty(AtomikosDataSourceBean datasource, String key) {
        String value = datasource.getXaProperties().getProperty(key);
        return value;
    }

    public static int getIntProperty(AtomikosDataSourceBean datasource, String key, int defaultValue) {
        String value = getProperty(datasource, key);
        if (NumberUtils.isDigits(value)) {
            return Integer.valueOf(value);
        }
        return defaultValue;
    }

    public static boolean getBooleanProperty(AtomikosDataSourceBean datasource, String key, boolean defaultValue) {
        String value = getProperty(datasource, key);
        if (value != null) {
            return Boolean.valueOf(value);
        }
        return defaultValue;
    }

    public static boolean validate(AtomikosDataSourceBean sourceDataSource) {
        try {
            String url = getUrl(sourceDataSource);
            String username = getUsername(sourceDataSource);
            boolean contains = GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, username));
            if (!contains) {
                return GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, null));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean shadowTable(AtomikosDataSourceBean sourceDataSource) {
        try {
            String url = getUrl(sourceDataSource);
            String username = getUsername(sourceDataSource);
            return DatabaseUtils.isTestTable(url, username);
        } catch (Throwable e) {
            return true;
        }
    }

    /**
     * 判断是否是影子数据源
     *
     * @param target 目标数据源
     * @return
     */
    private static boolean isPerformanceDataSource(AtomikosDataSourceBean target) {
        for (Map.Entry<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource> entry : pressureDataSources.entrySet()) {
            AtomikosDataSourceBeanMediaDataSource mediatorDataSource = entry.getValue();
            if (mediatorDataSource.getDataSourcePerformanceTest() == null) {
                continue;
            }
            if (StringUtils.equals(getUsername(mediatorDataSource.getDataSourcePerformanceTest()), getUsername(target))
                    && StringUtils.equals(getUrl(mediatorDataSource.getDataSourcePerformanceTest()), getUrl(target))) {
                return true;
            }
        }
        return false;
    }

    //  static AtomicBoolean inited = new AtomicBoolean(false);

    public static void init(DataSourceMeta<AtomikosDataSourceBean> dataSourceMeta) {
        if (pressureDataSources.containsKey(dataSourceMeta) && pressureDataSources.get(dataSourceMeta) != null) {
            return;
        }
        AtomikosDataSourceBean target = dataSourceMeta.getDataSource();
        if (isPerformanceDataSource(target)) {
            return;
        }
        if (!validate(target)) {
            //没有配置对应的影子表或影子库
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0002")
                    .setMessage("没有配置对应的影子表或影子库！")
                    .setDetail("[atomikos] DataSourceWrapUtil:业务库配置:::url: " + target.getXaProperties().getProperty("URL")  + "username：" + dataSourceMeta.getUsername() + "; 中间件类型：other")
                    .report();

            AtomikosDataSourceBeanMediaDataSource dbMediatorDataSource = new AtomikosDataSourceBeanMediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);

            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                if (isInfoEnabled) {
                    logger.info("[atomikos] destroyed shadow table datasource success. url:{} ,username:{}", target.getXaProperties().getProperty("URL"), target.getXaProperties().getProperty("user"));
                }
                old.close();
            }
            return;
        }
        if (shadowTable(target)) {
            //影子表
            try {
                AtomikosDataSourceBeanMediaDataSource dbMediatorDataSource = new AtomikosDataSourceBeanMediaDataSource();
                dbMediatorDataSource.setDataSourceBusiness(target);

                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
                if (old != null) {
                    if (isInfoEnabled) {
                        logger.info("[atomikos] destroyed shadow table datasource success. url:{} ,username:{}", target.getXaProperties().getProperty("URL"), target.getXaProperties().getProperty("user"));
                    }
                    old.close();
                }
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子表设置初始化异常！")
                        .setDetail("dbcp:DataSourceWrapUtil:业务库配置:::url: " + target.getXaProperties().getProperty("URL") + "|||" + Throwables.getStackTraceAsString(e))
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
                logger.error("[atomikos] init datasource err!", e);
            }
        } else {
            //影子库
            try {
                AtomikosDataSourceBeanMediaDataSource dataSource = new AtomikosDataSourceBeanMediaDataSource();
                AtomikosDataSourceBean ptDataSource = copy(target);
                dataSource.setDataSourcePerformanceTest(ptDataSource);
                dataSource.setDataSourceBusiness(target);

                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dataSource);
                if (old != null) {
                    if (isInfoEnabled) {
                        logger.info("[atomikos] destroyed shadow table datasource success. url:{} ,username:{}", getUrl(target), getUsername(target));
                    }
                    old.close();
                }
                if (isInfoEnabled) {
                    logger.info("[atomikos] create shadow datasource success. target:{} url:{} ,username:{} shadow-url:{},shadow-username:{}", target.hashCode(), getUrl(target), getUsername(target), getUrl(ptDataSource), getUsername(ptDataSource));
                }
            } catch (Throwable t) {
                logger.error("[atomikos] init datasource err!", t);
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子库设置初始化异常！")
                        .setDetail("[atomikos] DataSourceWrapUtil:业务库配置:::url: " + getUrl(target) + "|||" + Throwables.getStackTraceAsString(t))
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
            }
        }
    }

    private static AtomikosDataSourceBean copy(AtomikosDataSourceBean source) {
        AtomikosDataSourceBean target = generate(source);
        return target;
    }


    public static AtomikosDataSourceBean generate(AtomikosDataSourceBean sourceDatasource) {
        Map<String, ShadowDatabaseConfig> conf = GlobalConfig.getInstance().getShadowDatasourceConfigs();
        if (conf == null) {
            return null;
        }
        ShadowDatabaseConfig ptDataSourceConf
                = selectMatchPtDataSourceConfiguration(sourceDatasource, conf);
        if (ptDataSourceConf == null) {
            return null;
        }
        String driverClassName = ptDataSourceConf.getShadowDriverClassName();
        String url = ptDataSourceConf.getShadowUrl();
        String username = ptDataSourceConf.getShadowUsername();
        String password = ptDataSourceConf.getShadowPassword();
        String xaDataSourceClassName = ptDataSourceConf.getProperty("xaDataSourceClassName");

        if (StringUtils.isBlank(driverClassName)) {
            driverClassName = getDriverClassName(sourceDatasource);
        }

        /**
         * 如果是 mysql 需要检查一下参数中是否有pinGlobalTxToPhysicalConnection=true
         */
        if (JdbcUtils.isMySqlDriver(driverClassName)) {
            if (StringUtils.indexOf(url, "pinGlobalTxToPhysicalConnection") == -1) {
                if (StringUtils.indexOf(url, '?') != -1) {
                    url += "&pinGlobalTxToPhysicalConnection=true";
                } else {
                    url += "?pinGlobalTxToPhysicalConnection=true";
                }
            }
        }

        if (StringUtils.isBlank(url) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            logger.error("ATOMIKOS: url/username/password/xaDataSourceClassName can't not be null,{} {} {} {}", url, username, password);
            return null;
        }
        if (xaDataSourceClassName == null) {
            logger.warn("ATOMIKOS: xaDataSourceClassName is empty. use business xaDataSourceClassName:{}", sourceDatasource.getXaDataSourceClassName());
            xaDataSourceClassName = sourceDatasource.getXaDataSourceClassName();
        }

        Class clazz = null;
        try {
            clazz = Class.forName(xaDataSourceClassName);
        } catch (Throwable e) {
            logger.warn("ATOMIKOS: can't load class xaDataSourceClassName:{}", sourceDatasource.getXaDataSourceClassName(), e);
            return null;
        }

        AtomikosDataSourceBean target = new AtomikosDataSourceBean();
        Properties properties = new Properties();

        if (hasField(clazz, "URL")) {
            properties.put("URL", url);
        } else if (hasField(clazz, "url")) {
            properties.put("url", url);
        }
        if (hasField(clazz, "user")) {
            properties.put("user", username);
        }
        if (hasField(clazz, "username")) {
            properties.put("username", username);
        } else if (hasField(clazz, "user")) {
            properties.put("user", username);
        } else if (hasField(clazz, "User")) {
            properties.put("User", username);
        }

        if (hasField(clazz, "password")) {
            properties.put("password", password);
        }

        if (hasField(clazz, "driverClassName")) {
            properties.put("driverClassName", driverClassName);
        } else if (hasField(clazz, "driverClass")) {
            properties.put("driverClass", driverClassName);
        } else if (hasField(clazz, "driver")) {
            properties.put("driver", driverClassName);
        }

        if (hasField(clazz, "initialSize")) {
            Integer initialSize = ptDataSourceConf.getIntProperty("initialSize", getIntProperty(sourceDatasource, "initialSize", 1));
            properties.put("initialSize", initialSize);
        }

        if (hasField(clazz, "minIdle")) {
            Integer minIdle = ptDataSourceConf.getIntProperty("minIdle", getIntProperty(sourceDatasource, "minIdle", 1));
            properties.put("minIdle", minIdle);
        }

        Integer maxActive = ptDataSourceConf.getIntProperty("maxActive");
        if (maxActive != null) {
            if (hasField(clazz, "maxActive")) {
                properties.put("maxActive", maxActive);
            }
        } else {
            if (hasField(clazz, "maxActive")) {
                final String maxActive1 = getProperty(sourceDatasource, "maxActive");
                if (NumberUtils.isDigits(maxActive1)) {
                    properties.put("maxActive", Integer.valueOf(maxActive1));
                }
            }
        }


        Integer maxWait = ptDataSourceConf.getIntProperty("maxWait");
        if (maxWait != null) {
            if (hasField(clazz, "maxWait")) {
                properties.put("maxWait", maxWait);
            }
        } else {
            if (hasField(clazz, "maxWait")) {
                final String maxWait1 = getProperty(sourceDatasource, "maxWait");
                if (NumberUtils.isDigits(maxWait1)) {
                    properties.put("maxWait", Integer.valueOf(maxWait1));
                }
            }
        }

        Integer timeBetweenEvictionRunsMillis = ptDataSourceConf.getIntProperty("timeBetweenEvictionRunsMillis");
        if (timeBetweenEvictionRunsMillis != null) {
            if (hasField(clazz, "timeBetweenEvictionRunsMillis")) {
                properties.put("timeBetweenEvictionRunsMillis", timeBetweenEvictionRunsMillis);
            }
        } else {
            if (hasField(clazz, "timeBetweenEvictionRunsMillis")) {
                final String timeBetweenEvictionRunsMillis1 = getProperty(sourceDatasource, "timeBetweenEvictionRunsMillis");
                if (NumberUtils.isDigits(timeBetweenEvictionRunsMillis1)) {
                    properties.put("timeBetweenEvictionRunsMillis", Integer.valueOf(timeBetweenEvictionRunsMillis1));
                }
            }
        }

        Integer minEvictableIdleTimeMillis = ptDataSourceConf.getIntProperty("minEvictableIdleTimeMillis");
        if (minEvictableIdleTimeMillis != null) {
            if (hasField(clazz, "minEvictableIdleTimeMillis")) {
                properties.put("minEvictableIdleTimeMillis", minEvictableIdleTimeMillis);
            }
        } else {
            if (hasField(clazz, "minEvictableIdleTimeMillis")) {
                final String minEvictableIdleTimeMillis1 = getProperty(sourceDatasource, "minEvictableIdleTimeMillis");
                if (NumberUtils.isDigits(minEvictableIdleTimeMillis1)) {
                    properties.put("minEvictableIdleTimeMillis", Integer.valueOf(minEvictableIdleTimeMillis1));
                }
            }
        }

        Integer validationQueryTimeout = ptDataSourceConf.getIntProperty("validationQueryTimeout");
        if (validationQueryTimeout != null) {
            if (hasField(clazz, "validationQueryTimeout")) {
                properties.put("validationQueryTimeout", validationQueryTimeout);
            }
        } else {
            if (hasField(clazz, "validationQueryTimeout")) {
                final String validationQueryTimeout1 = getProperty(sourceDatasource, "validationQueryTimeout");
                if (validationQueryTimeout1 != null) {
                    properties.put("validationQueryTimeout", validationQueryTimeout1);
                }
            }
        }

        String validationQuery = ptDataSourceConf.getProperty("validationQuery");
        if (validationQuery != null) {
            if (hasField(clazz, "validationQuery")) {
                properties.put("validationQuery", validationQuery);
            }
        } else {
            if (hasField(clazz, "validationQuery")) {
                final String validationQuery1 = getProperty(sourceDatasource, "validationQuery");
                if (validationQuery1 != null) {
                    properties.put("validationQuery", validationQuery1);
                }
            }
        }

        Boolean testWhileIdle = ptDataSourceConf.getBooleanProperty("testWhileIdle");
        if (testWhileIdle != null) {
            if (hasField(clazz, "testWhileIdle")) {
                properties.put("testWhileIdle", testWhileIdle);
            }
        } else {
            if (hasField(clazz, "testWhileIdle")) {
                final String testWhileIdle1 = getProperty(sourceDatasource, "testWhileIdle");
                if (testWhileIdle1 != null) {
                    properties.put("testWhileIdle", Boolean.valueOf(testWhileIdle1));
                }
            }
        }

        Boolean testOnBorrow = ptDataSourceConf.getBooleanProperty("testOnBorrow");
        if (testOnBorrow != null) {
            if (hasField(clazz, "testOnBorrow")) {
                properties.put("testOnBorrow", testOnBorrow);
            }
        } else {
            if (hasField(clazz, "testOnBorrow")) {
                final String testOnBorrow1 = getProperty(sourceDatasource, "testOnBorrow");
                if (testOnBorrow1 != null) {
                    properties.put("testOnBorrow", Boolean.valueOf(testOnBorrow1));
                }
            }
        }

        Boolean testOnReturn = ptDataSourceConf.getBooleanProperty("testOnReturn");
        if (testOnReturn != null) {
            if (hasField(clazz, "testOnReturn")) {
                properties.put("testOnReturn", testOnReturn);
            }
        } else {
            if (hasField(clazz, "testOnReturn")) {
                final String testOnReturn1 = getProperty(sourceDatasource, "testOnReturn");
                if (testOnReturn1 != null) {
                    properties.put("testOnReturn", Boolean.valueOf(testOnReturn1));
                }
            }
        }

        Boolean poolPreparedStatements = ptDataSourceConf.getBooleanProperty("poolPreparedStatements");
        if (poolPreparedStatements != null) {
            if (hasField(clazz, "poolPreparedStatements")) {
                properties.put("poolPreparedStatements", poolPreparedStatements);
            }
        } else {
            if (hasField(clazz, "poolPreparedStatements")) {
                final String poolPreparedStatements1 = getProperty(sourceDatasource, "poolPreparedStatements");
                if (poolPreparedStatements1 != null) {
                    properties.put("poolPreparedStatements", Boolean.valueOf(poolPreparedStatements1));
                }
            }
        }

        Integer maxPoolPreparedStatementPerConnectionSize = ptDataSourceConf.getIntProperty("maxPoolPreparedStatementPerConnectionSize");
        if (maxPoolPreparedStatementPerConnectionSize != null) {
            if (hasField(clazz, "maxPoolPreparedStatementPerConnectionSize")) {
                properties.put("maxPoolPreparedStatementPerConnectionSize", maxPoolPreparedStatementPerConnectionSize);
            }
        } else {
            if (hasField(clazz, "maxPoolPreparedStatementPerConnectionSize")) {
                final String maxPoolPreparedStatementPerConnectionSize1 = getProperty(sourceDatasource, "maxPoolPreparedStatementPerConnectionSize");
                if (NumberUtils.isDigits(maxPoolPreparedStatementPerConnectionSize1)) {
                    properties.put("maxPoolPreparedStatementPerConnectionSize", Integer.valueOf(maxPoolPreparedStatementPerConnectionSize1));
                }
            }
        }

        Boolean pinGlobalTxToPhysicalConnection = ptDataSourceConf.getBooleanProperty("pinGlobalTxToPhysicalConnection");
        if (pinGlobalTxToPhysicalConnection != null) {
            if (hasField(clazz, "pinGlobalTxToPhysicalConnection")) {
                properties.put("pinGlobalTxToPhysicalConnection", pinGlobalTxToPhysicalConnection);
            }
        } else {
            if (hasField(clazz, "pinGlobalTxToPhysicalConnection")) {
                final String pinGlobalTxToPhysicalConnection1 = getProperty(sourceDatasource, "pinGlobalTxToPhysicalConnection");
                if (NumberUtils.isDigits(pinGlobalTxToPhysicalConnection1)) {
                    properties.put("pinGlobalTxToPhysicalConnection", Integer.valueOf(pinGlobalTxToPhysicalConnection1));
                }
            }
        }

        String filters = ptDataSourceConf.getProperty("filters");
        if (filters != null) {
            if (hasField(clazz, "filters")) {
                properties.put("filters", filters);
            }
        } else {
            if (hasField(clazz, "filters")) {
                final String filters1 = getProperty(sourceDatasource, "filters");
                if (filters1 != null) {
                    properties.put("filters", filters1);
                }
            }
        }

        String connectionProperties = ptDataSourceConf.getProperty("connectionProperties");
        if (connectionProperties != null) {
            if (hasField(clazz, "connectionProperties")) {
                properties.put("connectionProperties", connectionProperties);
            }
        } else {
            if (hasField(clazz, "connectionProperties")) {
                final String connectionProperties1 = getProperty(sourceDatasource, "connectionProperties");
                if (connectionProperties1 != null) {
                    properties.put("connectionProperties", connectionProperties1);
                }
            }
        }

        target.setXaProperties(properties);
        target.setXaDataSourceClassName(xaDataSourceClassName);

        Integer minPoolSize = ptDataSourceConf.getIntProperty("minPoolSize");
        if (minPoolSize != null) {
            target.setMinPoolSize(minPoolSize);
        } else {
            target.setMinPoolSize(getIntProperty(sourceDatasource, "minPoolSize", 1));
        }

        Integer maxPoolSize = ptDataSourceConf.getIntProperty("maxPoolSize");
        if (maxPoolSize != null) {
            target.setMaxPoolSize(maxPoolSize);
        } else {
            target.setMaxPoolSize(getIntProperty(sourceDatasource, "maxPoolSize", 1));
        }

        Integer poolSize = ptDataSourceConf.getIntProperty("poolSize");
        if (poolSize != null) {
            target.setPoolSize(poolSize);
        } else {
            target.setPoolSize(getIntProperty(sourceDatasource, "poolSize", 1));
        }

        Integer borrowConnectionTimeout = ptDataSourceConf.getIntProperty("borrowConnectionTimeout");
        if (borrowConnectionTimeout != null) {
            target.setBorrowConnectionTimeout(borrowConnectionTimeout);
        } else {
            target.setBorrowConnectionTimeout(getIntProperty(sourceDatasource, "borrowConnectionTimeout", 30));
        }

        Integer reapTimeout = ptDataSourceConf.getIntProperty("reapTimeout");
        if (reapTimeout != null) {
            target.setReapTimeout(reapTimeout);
        } else {
            target.setReapTimeout(getIntProperty(sourceDatasource, "reapTimeout", 0));
        }

        Integer maintenanceInterval = ptDataSourceConf.getIntProperty("maintenanceInterval");
        if (maintenanceInterval != null) {
            target.setMaintenanceInterval(maintenanceInterval);
        } else {
            target.setMaintenanceInterval(getIntProperty(sourceDatasource, "maintenanceInterval", 60));
        }

        Integer maxIdleTime = ptDataSourceConf.getIntProperty("maxIdleTime");
        if (maxIdleTime != null) {
            target.setMaxIdleTime(Integer.valueOf(maxIdleTime));
        } else {
            target.setMaxIdleTime(getIntProperty(sourceDatasource, "maxIdleTime", 60));
        }

        Integer maxLifetime = ptDataSourceConf.getIntProperty("maxLifetime");
        if (maxLifetime != null) {
            target.setMaxLifetime(maxLifetime);
        } else {
            target.setMaxLifetime(getIntProperty(sourceDatasource, "maxLifetime", 0));
        }

        String testQuery = ptDataSourceConf.getProperty("testQuery");
        if (StringUtils.isNotBlank(testQuery)) {
            target.setTestQuery(testQuery);
        } else {
            target.setTestQuery(getProperty(sourceDatasource, "testQuery"));
        }

        Boolean concurrentConnectionValidation = ptDataSourceConf.getBooleanProperty("concurrentConnectionValidation");
        if (concurrentConnectionValidation != null) {
            target.setConcurrentConnectionValidation(concurrentConnectionValidation);
        } else {
            target.setConcurrentConnectionValidation(getBooleanProperty(sourceDatasource, "concurrentConnectionValidation", true));
        }

        String resourceName = ptDataSourceConf.getProperty("resourceName");
        if (StringUtils.isNotBlank(resourceName)) {
            target.setUniqueResourceName(resourceName);
        } else {
            target.setUniqueResourceName(Pradar.addClusterTestSuffixRodLower(Pradar.addClusterTestPrefixRodLower(sourceDatasource.getUniqueResourceName())));
        }

        Integer defaultIsolationLevel = ptDataSourceConf.getIntProperty("defaultIsolationLevel");
        if (defaultIsolationLevel != null) {
            target.setDefaultIsolationLevel(defaultIsolationLevel);
        } else {
            target.setDefaultIsolationLevel(getIntProperty(sourceDatasource, "defaultIsolationLevel", -1));
        }

        return target;
    }

    private static ShadowDatabaseConfig selectMatchPtDataSourceConfiguration(AtomikosDataSourceBean source, Map<String, ShadowDatabaseConfig> shadowDbConfigurations) {
        String key = DbUrlUtils.getKey(getUrl(source), getUsername(source));
        ShadowDatabaseConfig shadowDatabaseConfig = shadowDbConfigurations.get(key);
        if (shadowDatabaseConfig == null) {
            key = DbUrlUtils.getKey(getUrl(source), null);
            shadowDatabaseConfig = shadowDbConfigurations.get(key);
        }
        return shadowDatabaseConfig;
    }

    public static boolean hasField(Class clazz, String propertyName) {
        try {
            String setterName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            Method[] methods = clazz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (method.getName().equals(setterName) && method.getReturnType().equals(void.class) && method.getParameterTypes().length == 1) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            return false;
        }
    }
}
