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
package com.pamirs.attach.plugin.proxool.utils;

import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.datasource.DatabaseUtils;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import org.apache.commons.lang.StringUtils;
import org.logicalcobwebs.proxool.ProxoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: vernon
 * @Date: 2020/3/29 14:59
 * @Description:
 */
public class DataSourceWrapUtil {
    private static Logger logger = LoggerFactory.getLogger(DataSourceWrapUtil.class.getName());

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
            String url = sourceDataSource.getDriverUrl();
            String username = sourceDataSource.getUser();
            boolean contains = GlobalConfig.getInstance().containsShadowDatabaseConfig(
                    DbUrlUtils.getKey(url, username));
            if (!contains) {
                return GlobalConfig.getInstance().containsShadowDatabaseConfig(DbUrlUtils.getKey(url, null));
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean shadowTable(ProxoolDataSource sourceDataSource) {
        try {
            String url = sourceDataSource.getDriverUrl();
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
        if (!validate(target)) {
            logger.error("[proxool] No configuration found for datasource, url: " + target.getDriverUrl());
            //没有配置对应的影子表或影子库
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0002")
                    .setMessage("没有配置对应的影子表或影子库！")
                    .setDetail("proxool:DataSourceWrapUtil:业务库配置:::url: " + target.getDriverUrl())
                    .report();

            ProxoolMediaDataSource dbMediatorDataSource = new ProxoolMediaDataSource();
            dbMediatorDataSource.setDataSourceBusiness(target);
            DbMediatorDataSource old = pressureDataSources.put(dataSourceMeta, dbMediatorDataSource);
            if (old != null) {
                logger.info("[proxool] destroyed shadow table datasource success. url:{} ,username:{}",
                        target.getDriverUrl(), target.getUser());
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
                    logger.info("[proxool] destroyed shadow table datasource success. url:{} ,username:{}",
                            target.getDriverUrl(), target.getUser());
                    old.close();
                }
            } catch (Throwable e) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0002")
                        .setMessage("没有配置对应的影子表或影子库！")
                        .setDetail("proxool:DataSourceWrapUtil:业务库配置:::url: " +
                                target.getDriverUrl() + Throwables.getStackTraceAsString(e))
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
                    logger.info("[proxool] destroyed shadow table datasource success. url:{} ,username:{}",
                            target.getDriverUrl(), target.getUser());
                    old.close();
                }
                logger.info(
                        "[proxool] create shadow datasource success. target:{} url:{} ,username:{} shadow-url:{},"
                                + "shadow-username:{}",
                        target.hashCode(), target.getDriverUrl(), target.getUser(), ptDataSource.getDriverUrl(),
                        ptDataSource.getUser());
            } catch (Throwable t) {
                logger.error("[proxool] init datasource err!", t);
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0003")
                        .setMessage("影子库初始化失败！")
                        .setDetail("proxool:DataSourceWrapUtil:业务库配置:::url: " +
                                target.getDriverUrl() + Throwables.getStackTraceAsString(t))
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
            }
        }
    }

    private static ProxoolDataSource copy(ProxoolDataSource source) {
        ProxoolDataSource target = generate(source);
        return target;
    }

    public static ProxoolDataSource generate(ProxoolDataSource sourceDatasource) {
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

        if (StringUtils.isBlank(url) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }
        String driverClassName = ptDataSourceConf.getShadowDriverClassName();
        if (StringUtils.isBlank(driverClassName)) {
            driverClassName = sourceDatasource.getDriver();
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
            target.setFatalSqlExceptionsAsString(sourceDatasource.getFatalSqlExceptionsAsString());
        }

        String fatalSqlExceptionWrapperClass = ptDataSourceConf.getProperty("fatal-sql-exception-wrapper-class");
        if (fatalSqlExceptionWrapperClass != null) {
            target.setFatalSqlExceptionWrapperClass(fatalSqlExceptionWrapperClass);
        } else {
            target.setFatalSqlExceptionWrapperClass(sourceDatasource.getFatalSqlExceptionWrapperClass());
        }

        Integer houseKeepingSleepTime = ptDataSourceConf.getIntProperty("house-keeping-sleep-time");
        if (houseKeepingSleepTime != null) {
            target.setHouseKeepingSleepTime(houseKeepingSleepTime);
        } else {
            target.setHouseKeepingSleepTime((int) sourceDatasource.getHouseKeepingSleepTime());
        }

        String houseKeepingTestSql = ptDataSourceConf.getProperty("house-keeping-test-sql");
        if (houseKeepingTestSql != null) {
            target.setHouseKeepingTestSql(houseKeepingTestSql);
        } else {
            target.setHouseKeepingTestSql(sourceDatasource.getHouseKeepingTestSql());
        }

        Boolean jmx = ptDataSourceConf.getBooleanProperty("jmx");
        if (jmx != null) {
            target.setJmx(jmx);
        } else {
            target.setJmx(sourceDatasource.isJmx());
        }

        String jmxAgentId = ptDataSourceConf.getProperty("jmx-agent-id");
        if (jmxAgentId != null) {
            target.setJmxAgentId(jmxAgentId);
        } else {
            target.setJmxAgentId(sourceDatasource.getJmxAgentId());
        }

        Integer maximumActiveTime = ptDataSourceConf.getIntProperty("maximum-active-time");
        if (maximumActiveTime != null) {
            target.setMaximumActiveTime(maximumActiveTime);
        } else {
            target.setMaximumActiveTime(sourceDatasource.getMaximumActiveTime());
        }

        Integer maximumConnectionCount = ptDataSourceConf.getIntProperty("maximum-connection-count");
        if (maximumConnectionCount != null) {
            target.setMaximumConnectionCount(maximumConnectionCount);
        } else {
            target.setMaximumConnectionCount(sourceDatasource.getMaximumConnectionCount());
        }

        Integer maximumConnectionLifetime = ptDataSourceConf.getIntProperty("maximum-connection-lifetime");
        if (maximumConnectionLifetime != null) {
            target.setMaximumConnectionLifetime(maximumConnectionLifetime);
        } else {
            target.setMaximumConnectionLifetime((int) sourceDatasource.getMaximumConnectionLifetime());
        }

        Integer maximumNewConnections = ptDataSourceConf.getIntProperty("maximum-new-connections");
        if (maximumNewConnections != null) {
            target.setMaximumConnectionCount(maximumNewConnections);
        } else {
            target.setMaximumConnectionCount(sourceDatasource.getMaximumConnectionCount());
        }

        Integer minimumConnectionCount = ptDataSourceConf.getIntProperty("minimum-connection-count");
        if (minimumConnectionCount != null) {
            target.setMinimumConnectionCount(minimumConnectionCount);
        } else {
            target.setMinimumConnectionCount(sourceDatasource.getMinimumConnectionCount());
        }

        Integer overloadWithoutRefusalLifetime = ptDataSourceConf.getIntProperty("overload-without-refusal-lifetime");
        if (overloadWithoutRefusalLifetime != null) {
            target.setOverloadWithoutRefusalLifetime(overloadWithoutRefusalLifetime);
        } else {
            target.setOverloadWithoutRefusalLifetime((int) sourceDatasource.getOverloadWithoutRefusalLifetime());
        }

        Integer prototypeCount = ptDataSourceConf.getIntProperty("prototype-count");
        if (prototypeCount != null) {
            target.setPrototypeCount(prototypeCount);
        } else {
            target.setPrototypeCount(sourceDatasource.getPrototypeCount());
        }

        Integer recentlyStartedThreshold = ptDataSourceConf.getIntProperty("recently-started-threshold");
        if (recentlyStartedThreshold != null) {
            target.setRecentlyStartedThreshold(recentlyStartedThreshold);
        } else {
            target.setRecentlyStartedThreshold((int) sourceDatasource.getRecentlyStartedThreshold());
        }

        Integer simultaneousBuildThrottle = ptDataSourceConf.getIntProperty("simultaneous-build-throttle");
        if (simultaneousBuildThrottle != null) {
            target.setSimultaneousBuildThrottle(simultaneousBuildThrottle);
        } else {
            target.setSimultaneousBuildThrottle(sourceDatasource.getSimultaneousBuildThrottle());
        }

        String statistics = ptDataSourceConf.getProperty("statistics");
        if (statistics != null) {
            target.setStatistics(statistics);
        } else {
            target.setStatistics(sourceDatasource.getStatistics());
        }

        String statisticsLogLevel = ptDataSourceConf.getProperty("statistics-log-level");
        if (statisticsLogLevel != null) {
            target.setStatisticsLogLevel(statisticsLogLevel);
        } else {
            target.setStatisticsLogLevel(sourceDatasource.getStatisticsLogLevel());
        }

        Boolean testBeforeUse = ptDataSourceConf.getBooleanProperty("test-before-use");
        if (testBeforeUse != null) {
            target.setTestBeforeUse(testBeforeUse);
        } else {
            target.setTestBeforeUse(sourceDatasource.isTestBeforeUse());
        }

        Boolean testAfterUse = ptDataSourceConf.getBooleanProperty("test-after-use");
        if (testAfterUse != null) {
            target.setTestAfterUse(testAfterUse);
        } else {
            target.setTestAfterUse(sourceDatasource.isTestAfterUse());
        }

        Boolean trace = ptDataSourceConf.getBooleanProperty("trace");
        if (trace != null) {
            target.setTrace(trace);
        } else {
            target.setTrace(sourceDatasource.isTrace());
        }

        Boolean verbose = ptDataSourceConf.getBooleanProperty("verbose");
        if (verbose != null) {
            target.setVerbose(verbose);
        } else {
            target.setVerbose(sourceDatasource.isVerbose());
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    private static ShadowDatabaseConfig selectMatchPtDataSourceConfiguration(ProxoolDataSource source,
                                                                             Map<String, ShadowDatabaseConfig> shadowDbConfigurations) {
        ProxoolDataSource dataSource = source;
        String key = DbUrlUtils.getKey(dataSource.getDriverUrl(), dataSource.getUser());
        ShadowDatabaseConfig shadowDatabaseConfig = shadowDbConfigurations.get(key);
        if (shadowDatabaseConfig == null) {
            key = DbUrlUtils.getKey(dataSource.getDriverUrl(), null);
            shadowDatabaseConfig = shadowDbConfigurations.get(key);
        }
        return shadowDatabaseConfig;
    }
}
