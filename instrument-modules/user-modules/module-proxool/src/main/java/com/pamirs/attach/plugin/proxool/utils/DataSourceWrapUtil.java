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
package com.pamirs.attach.plugin.proxool.utils;

import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DatabaseUtils;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.apache.commons.lang.StringUtils;
import org.logicalcobwebs.proxool.ProxoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: vernon
 * @Date: 2020/3/29 14:59
 * @Description:
 */
public class DataSourceWrapUtil {
    private static Logger logger = LoggerFactory.getLogger(DataSourceWrapUtil.class.getName());
    private final static Object lock = new Object();

    public static final ConcurrentHashMap<DataSourceMeta, ProxoolMediaDataSource> pressureDataSources
            = new ConcurrentHashMap<DataSourceMeta, ProxoolMediaDataSource>();

    public static void destroy() {
        Iterator<Map.Entry<DataSourceMeta, ProxoolMediaDataSource>> it = pressureDataSources.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<DataSourceMeta, ProxoolMediaDataSource> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        pressureDataSources.clear();
    }

    public static boolean validate(ProxoolDataSource sourceDataSource) {
        try {
            String[] configKeys = extractConfigKeys(sourceDataSource);
            boolean contains = GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(configKeys[0], configKeys[1]));
            if (!contains) {
                return GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(configKeys[0], null));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String[] extractConfigKeys(ProxoolDataSource sourceDataSource) throws ClassNotFoundException {
        String url = sourceDataSource.getDriverUrl();
        String username = sourceDataSource.getUser();
        if (StringUtils.isNotBlank(url) && StringUtils.isNotBlank(username)) {
            return new String[]{url, username};
        }

        Object definition = extractDefinition(sourceDataSource);
        url = ReflectionUtils.get(definition, "url");
        Properties delegateProperties = ReflectionUtils.get(definition, "delegateProperties");
        String user = delegateProperties.getProperty("user");
        return new String[]{url, user};
    }

    public static Object extractDefinition(ProxoolDataSource sourceDataSource) throws ClassNotFoundException {
        return extractDefinition(sourceDataSource.getClass(), ReflectionUtils.<String>get(sourceDataSource, "alias"));
    }

    public static Object extractDefinition(Class bizClass, String alias) throws ClassNotFoundException {
        Class clazz = bizClass.getClassLoader().loadClass("org.logicalcobwebs.proxool.ConnectionPoolManager");
        Object manager = ReflectionUtils.getStatic(clazz, "connectionPoolManager");
        Map connectionPoolMap = ReflectionUtils.getField(ReflectionUtils.findField(clazz, "connectionPoolMap"), manager);
        Object connectionPool = connectionPoolMap.get(alias);
        return ReflectionUtils.get(connectionPool, "definition");
    }

    public static boolean shadowTable(ProxoolDataSource sourceDataSource) {
        try {
            String[] configKeys = extractConfigKeys(sourceDataSource);
            return DatabaseUtils.isTestTable(configKeys[0], configKeys[1]);
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
    private static boolean isPerformanceDataSource(ProxoolDataSource target) {
        for (Map.Entry<DataSourceMeta, ProxoolMediaDataSource> entry : pressureDataSources.entrySet()) {
            ProxoolMediaDataSource mediatorDataSource = entry.getValue();
            if (mediatorDataSource.getDataSourcePerformanceTest() == null) {
                continue;
            }
            if (StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getDriverUrl(),
                    target.getDriverUrl()) &&
                    StringUtils.equals(mediatorDataSource.getDataSourcePerformanceTest().getUser(), target.getUser())) {
                return true;
            }
        }
        return false;
    }

    //  static AtomicBoolean inited = new AtomicBoolean(false);

    public static void init(DataSourceMeta<ProxoolDataSource> dataSourceMeta) {
        if (pressureDataSources.containsKey(dataSourceMeta) && pressureDataSources.get(dataSourceMeta) != null) {
            return;
        }
        ProxoolDataSource target = dataSourceMeta.getDataSource();
        if (isPerformanceDataSource(target)) {
            return;
        }
        synchronized (lock) {
            if (pressureDataSources.containsKey(dataSourceMeta) && pressureDataSources.get(dataSourceMeta) != null) {
                return;
            }
            if (!validate(target)) {
                logger.error("[proxool] No configuration found for datasource, url: " + target.getDriverUrl());
                //没有配置对应的影子表或影子库
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0002")
                        .setMessage("没有配置对应的影子表或影子库！")
                        .setDetail("proxool:DataSourceWrapUtil:业务库配置:::url: " + target.getDriverUrl() + "; username: " + target.getUser())
                        .report();

                ProxoolMediaDataSource dbMediatorDataSource = new ProxoolMediaDataSource();
                dbMediatorDataSource.setDataSourceBusiness(target);
                DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
                if (old != null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("[proxool] destroyed shadow table datasource success. url:{} ,username:{}",
                                target.getDriverUrl(), target.getUser());
                    }
                    old.close();
                }
                return;
            }
            if (shadowTable(target)) {
                //影子表
                try {
                    ProxoolMediaDataSource dbMediatorDataSource = new ProxoolMediaDataSource();
                    dbMediatorDataSource.setDataSourceBusiness(target);
                    DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
                    if (old != null) {
                        if (logger.isInfoEnabled()) {
                            logger.info("[proxool] destroyed shadow table datasource success. url:{} ,username:{}",
                                    target.getDriverUrl(), target.getUser());
                        }
                        old.close();
                    }
                } catch (Throwable e) {
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.DataSource)
                            .setErrorCode("datasource-0002")
                            .setMessage("没有配置对应的影子表或影子库！")
                            .setDetail("proxool:DataSourceWrapUtil:业务库配置:::url: " + target.getDriverUrl() + "; username: " + target.getUser())
                            .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                            .report();
                    logger.error("[proxool] init datasource err!", e);
                }
            } else {
                //影子库
                try {
                    ProxoolMediaDataSource dataSource = new ProxoolMediaDataSource();
                    /**
                     * 如果没有配置则为null
                     */
                    ProxoolDataSource ptDataSource = copy(target);
                    dataSource.setDataSourcePerformanceTest(ptDataSource);
                    dataSource.setDataSourceBusiness(target);
                    DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dataSource);
                    if (old != null) {
                        if (logger.isInfoEnabled()) {
                            logger.info("[proxool] destroyed shadow table datasource success. url:{} ,username:{}",
                                    target.getDriverUrl(), target.getUser());
                        }
                        old.close();
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info(
                                "[proxool] create shadow datasource success. target:{} url:{} ,username:{} shadow-url:{},"
                                        + "shadow-username:{}",
                                target.hashCode(), target.getDriverUrl(), target.getUser(), ptDataSource.getDriverUrl(),
                                ptDataSource.getUser());
                    }
                } catch (Throwable t) {
                    logger.error("[proxool] init datasource err!", t);
                    ErrorReporter.buildError()
                            .setErrorType(ErrorTypeEnum.DataSource)
                            .setErrorCode("datasource-0003")
                            .setMessage("影子库初始化失败！")
                            .setDetail("proxool:DataSourceWrapUtil:业务库配置:::url: " + target.getDriverUrl() + "; username: " + target.getUser())
                            .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                            .report();
                }
            }
        }
    }

    private static ProxoolDataSource copy(ProxoolDataSource source) throws ClassNotFoundException {
        ProxoolDataSource target = generate(source);
        return target;
    }

    public static ProxoolDataSource generate(ProxoolDataSource sourceDatasource) throws ClassNotFoundException {
        Map<String, ShadowDatabaseConfig> conf = GlobalConfig.getInstance().getShadowDatasourceConfigs();
        if (conf == null) {
            return null;
        }
        ShadowDatabaseConfig ptDataSourceConf
                = selectMatchPtDataSourceConfiguration(sourceDatasource, conf);
        if (ptDataSourceConf == null) {
            return null;
        }

        String[] configKeys = extractConfigKeys(sourceDatasource);

        String url = ptDataSourceConf.getShadowUrl();
        String username = ptDataSourceConf.getShadowUsername(configKeys[1]);
        String password = ptDataSourceConf.getShadowPassword(sourceDatasource.getPassword());

        if (StringUtils.isBlank(url) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }

        Object definition = extractDefinition(sourceDatasource);
        // 依赖是否时proxool:proxool 还是 com.cloudhopper.proxool:proxool
        boolean applySource = sourceDatasource.getDriverUrl() != null;

        String driverClassName = ptDataSourceConf.getShadowDriverClassName();
        if (StringUtils.isBlank(driverClassName)) {
            driverClassName = applySource ? sourceDatasource.getDriver() : ReflectionUtils.<String>get(definition, "driver");
        }

        ProxoolDataSource target = null;
        String datasourceName = sourceDatasource.getAlias();
        if (StringUtils.isNotBlank(datasourceName)) {
            try {
                target = new ProxoolDataSource(Pradar.addClusterTestPrefix(datasourceName));
            } catch (Throwable e) {

                target = new ProxoolDataSource();
            }
        } else {
            target = new ProxoolDataSource();
        }
        target.setDriverUrl(url);
        target.setUser(username);
        target.setPassword(password);
        target.setDriver(driverClassName);

        String checkoutTimeout = ptDataSourceConf.getProperty("fatal-sql-exception");
        if (checkoutTimeout != null) {
            target.setFatalSqlExceptionsAsString(checkoutTimeout);
        } else {
            target.setFatalSqlExceptionsAsString(applySource ? sourceDatasource.getFatalSqlExceptionsAsString() : ReflectionUtils.<String>get(definition, "fatalSqlExceptionsAsString"));
        }

        String fatalSqlExceptionWrapperClass = ptDataSourceConf.getProperty("fatal-sql-exception-wrapper-class");
        if (fatalSqlExceptionWrapperClass != null) {
            target.setFatalSqlExceptionWrapperClass(fatalSqlExceptionWrapperClass);
        } else {
            target.setFatalSqlExceptionWrapperClass(applySource ? sourceDatasource.getFatalSqlExceptionWrapperClass() : ReflectionUtils.<String>get(definition, "fatalSqlExceptionWrapper"));
        }

        Integer houseKeepingSleepTime = ptDataSourceConf.getIntProperty("house-keeping-sleep-time");
        if (houseKeepingSleepTime != null) {
            target.setHouseKeepingSleepTime(houseKeepingSleepTime);
        } else {
            target.setHouseKeepingSleepTime(applySource ? (int)sourceDatasource.getHouseKeepingSleepTime() : ReflectionUtils.<Long>get(definition, "houseKeepingSleepTime").intValue());
        }

        String houseKeepingTestSql = ptDataSourceConf.getProperty("house-keeping-test-sql");
        if (houseKeepingTestSql != null) {
            target.setHouseKeepingTestSql(houseKeepingTestSql);
        } else {
            target.setHouseKeepingTestSql(applySource ? sourceDatasource.getHouseKeepingTestSql() : ReflectionUtils.<String>get(definition, "houseKeepingTestSql"));
        }

        Boolean jmx = ptDataSourceConf.getBooleanProperty("jmx");
        if (jmx != null) {
            target.setJmx(jmx);
        } else {
            target.setJmx(applySource ? sourceDatasource.isJmx() : (Boolean) ReflectionUtils.get(definition, "jmx"));
        }

        String jmxAgentId = ptDataSourceConf.getProperty("jmx-agent-id");
        if (jmxAgentId != null) {
            target.setJmxAgentId(jmxAgentId);
        } else {
            target.setJmxAgentId(applySource ? sourceDatasource.getJmxAgentId() : ReflectionUtils.<String>get(definition, "jmxAgentId"));
        }

        Integer maximumActiveTime = ptDataSourceConf.getIntProperty("maximum-active-time");
        if (maximumActiveTime != null) {
            target.setMaximumActiveTime(maximumActiveTime);
        } else {
            target.setMaximumActiveTime(applySource ? sourceDatasource.getMaximumActiveTime() : ReflectionUtils.<Long>get(definition, "maximumActiveTime"));
        }

        Integer maximumConnectionCount = ptDataSourceConf.getIntProperty("maximum-connection-count");
        if (maximumConnectionCount != null) {
            target.setMaximumConnectionCount(maximumConnectionCount);
        } else {
            target.setMaximumConnectionCount(applySource ? sourceDatasource.getMaximumConnectionCount() : ReflectionUtils.<Integer>get(definition, "maximumConnectionCount"));
        }

        Integer maximumConnectionLifetime = ptDataSourceConf.getIntProperty("maximum-connection-lifetime");
        if (maximumConnectionLifetime != null) {
            target.setMaximumConnectionLifetime(maximumConnectionLifetime);
        } else {
            target.setMaximumConnectionLifetime(applySource ? (int) sourceDatasource.getMaximumConnectionLifetime() : ReflectionUtils.<Long>get(definition, "maximumConnectionLifetime").intValue());
        }

        Integer maximumNewConnections = ptDataSourceConf.getIntProperty("maximum-new-connections");
        if (maximumNewConnections != null) {
            target.setMaximumConnectionCount(maximumNewConnections);
        } else {
            target.setMaximumConnectionCount(applySource ? sourceDatasource.getMaximumConnectionCount() : ReflectionUtils.<Integer>get(definition, "maximumConnectionCount"));
        }

        Integer minimumConnectionCount = ptDataSourceConf.getIntProperty("minimum-connection-count");
        if (minimumConnectionCount != null) {
            target.setMinimumConnectionCount(minimumConnectionCount);
        } else {
            target.setMinimumConnectionCount(applySource ? sourceDatasource.getMinimumConnectionCount() : ReflectionUtils.<Integer>get(definition, "minimumConnectionCount"));
        }

        Integer overloadWithoutRefusalLifetime = ptDataSourceConf.getIntProperty("overload-without-refusal-lifetime");
        if (overloadWithoutRefusalLifetime != null) {
            target.setOverloadWithoutRefusalLifetime(overloadWithoutRefusalLifetime);
        } else {
            target.setOverloadWithoutRefusalLifetime(applySource ? (int) sourceDatasource.getOverloadWithoutRefusalLifetime() : ReflectionUtils.<Long>get(definition, "overloadWithoutRefusalLifetime").intValue());
        }

        Integer prototypeCount = ptDataSourceConf.getIntProperty("prototype-count");
        if (prototypeCount != null) {
            target.setPrototypeCount(prototypeCount);
        } else {
            target.setPrototypeCount(applySource ? sourceDatasource.getPrototypeCount() : ReflectionUtils.<Integer>get(definition, "prototypeCount"));
        }

        Integer recentlyStartedThreshold = ptDataSourceConf.getIntProperty("recently-started-threshold");
        if (recentlyStartedThreshold != null) {
            target.setRecentlyStartedThreshold(recentlyStartedThreshold);
        } else {
            target.setRecentlyStartedThreshold(applySource ? (int) sourceDatasource.getRecentlyStartedThreshold() : ReflectionUtils.<Long>get(definition, "recentlyStartedThreshold").intValue());
        }

        Integer simultaneousBuildThrottle = ptDataSourceConf.getIntProperty("simultaneous-build-throttle");
        if (simultaneousBuildThrottle != null) {
            target.setSimultaneousBuildThrottle(simultaneousBuildThrottle);
        } else {
            target.setSimultaneousBuildThrottle(applySource ? sourceDatasource.getSimultaneousBuildThrottle() : ReflectionUtils.<Integer>get(definition, "simultaneousBuildThrottle"));
        }

        String statistics = ptDataSourceConf.getProperty("statistics");
        if (statistics != null) {
            target.setStatistics(statistics);
        } else {
            target.setStatistics(applySource ? sourceDatasource.getStatistics() : ReflectionUtils.<String>get(definition, "statistics"));
        }

        String statisticsLogLevel = ptDataSourceConf.getProperty("statistics-log-level");
        if (statisticsLogLevel != null) {
            target.setStatisticsLogLevel(statisticsLogLevel);
        } else {
            target.setStatisticsLogLevel(applySource ? sourceDatasource.getStatisticsLogLevel() : ReflectionUtils.<String>get(definition, "statisticsLogLevel"));
        }

        Boolean testBeforeUse = ptDataSourceConf.getBooleanProperty("test-before-use");
        if (testBeforeUse != null) {
            target.setTestBeforeUse(testBeforeUse);
        } else {
            target.setTestBeforeUse(applySource ? sourceDatasource.isTestBeforeUse() : ReflectionUtils.<Boolean>get(definition, "testBeforeUse"));
        }

        Boolean testAfterUse = ptDataSourceConf.getBooleanProperty("test-after-use");
        if (testAfterUse != null) {
            target.setTestAfterUse(testAfterUse);
        } else {
            target.setTestAfterUse(applySource ? sourceDatasource.isTestAfterUse() : ReflectionUtils.<Boolean>get(definition, "testAfterUse"));
        }

        Boolean trace = ptDataSourceConf.getBooleanProperty("trace");
        if (trace != null) {
            target.setTrace(trace);
        } else {
            target.setTrace(applySource ? sourceDatasource.isTrace() : ReflectionUtils.<Boolean>get(definition, "trace"));
        }

        Boolean verbose = ptDataSourceConf.getBooleanProperty("verbose");
        if (verbose != null) {
            target.setVerbose(verbose);
        } else {
            target.setVerbose(applySource ? sourceDatasource.isVerbose() : ReflectionUtils.<Boolean>get(definition, "verbose"));
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    private static ShadowDatabaseConfig selectMatchPtDataSourceConfiguration(ProxoolDataSource source,
                                                                             Map<String, ShadowDatabaseConfig> shadowDbConfigurations) throws ClassNotFoundException {
        ProxoolDataSource dataSource = source;
        String[] configKeys = extractConfigKeys(dataSource);
        String key = DbUrlUtils.getKey(configKeys[0], configKeys[1]);
        ShadowDatabaseConfig shadowDatabaseConfig = shadowDbConfigurations.get(key);
        if (shadowDatabaseConfig == null) {
            key = DbUrlUtils.getKey(configKeys[0], null);
            shadowDatabaseConfig = shadowDbConfigurations.get(key);
        }
        return shadowDatabaseConfig;
    }
}
