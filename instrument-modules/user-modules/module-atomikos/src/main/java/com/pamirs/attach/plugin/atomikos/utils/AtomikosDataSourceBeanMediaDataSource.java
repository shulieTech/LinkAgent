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
import com.pamirs.attach.plugin.atomikos.connection.AtomikosBizConnection;
import com.pamirs.attach.plugin.atomikos.connection.AtomikosNormalConnection;
import com.pamirs.attach.plugin.atomikos.connection.AtomikosPressureConnection;
import com.pamirs.attach.plugin.common.datasource.WrappedDbMediatorDataSource;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.shulie.druid.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class AtomikosDataSourceBeanMediaDataSource extends WrappedDbMediatorDataSource<AtomikosDataSourceBean> implements DataSource {

    private final static Logger logger = LoggerFactory.getLogger(AtomikosDataSourceBeanMediaDataSource.class);


    @Override
    public String getUsername(AtomikosDataSourceBean datasource) {
        String username = datasource.getXaProperties().getProperty("user");
        if (username == null) {
            username = datasource.getXaProperties().getProperty("username");
        }
        if (username == null) {
            username = datasource.getXaProperties().getProperty("User");
        }
        return username;
    }

    @Override
    public String getUrl(AtomikosDataSourceBean datasource) {
        String url = datasource.getXaProperties().getProperty("URL");
        if (url == null) {
            url = datasource.getXaProperties().getProperty("url");
        }
        return url;
    }

    @Override
    public String getDriverClassName(AtomikosDataSourceBean datasource) {
        String driverClassName = datasource.getXaProperties().getProperty("driver");
        if (driverClassName == null) {
            driverClassName = datasource.getXaProperties().getProperty("driverClassName");
        }
        if (driverClassName == null) {
            driverClassName = datasource.getXaProperties().getProperty("driverClass");
        }
        return driverClassName;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (Pradar.isClusterTest()) {
            try {
                init();
                if (useTable) {
                    //影子表
                    if (dataSourceBusiness == null) {
                        throw new PressureMeasureError("[atomikos] Business dataSource is null.");
                    }
                    Connection connection = dataSourceBusiness.getConnection();
                    return new AtomikosNormalConnection(dataSourceBusiness, connection, dbConnectionKey, url, username, dbType);
                } else {
                    if (dataSourcePerformanceTest == null) {
                        throw new PressureMeasureError("[atomikos] pressure dataSource is null.");
                    }
                    return new AtomikosPressureConnection(dataSourcePerformanceTest, dataSourcePerformanceTest.getConnection(), getUrl(dataSourcePerformanceTest), getUsername(dataSourcePerformanceTest), dbConnectionKey, dbType);
                }
            } catch (Throwable e) {
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0001")
                        .setMessage("数据源获取链接失败！" + (Pradar.isClusterTest() ? "(压测流量)" : ""))
                        .setDetail("[atomikos] get connection failed by dbMediatorDataSource, message: " + e.getMessage() + "\r\n" + printStackTrace(e));
//                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                logger.error("ATOMIKOS: pressure test flow get connection fail.", e);
                error.report();
                throw new PressureMeasureError("pressure test flow get connection fail " + e.getMessage());
            }
        } else {
            final String url = getUrl(dataSourceBusiness);
            final String username = getUsername(dataSourceBusiness);
            String dbType = JdbcUtils.getDbType(url, JdbcUtils.getDriverClassName(url));
            return new AtomikosBizConnection(dataSourceBusiness.getConnection(), url, username, dbType);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        if (Pradar.isClusterTest()) {
            try {
                init();
                if (useTable) {
                    //影子表
                    if (dataSourceBusiness == null) {
                        throw new RuntimeException("[atomikos] Business dataSource is null.");
                    }
                    Connection connection = dataSourceBusiness.getConnection(username, password);
                    return new AtomikosNormalConnection(dataSourceBusiness, connection, dbConnectionKey, url, username, dbType);
                } else {
                    if (dataSourcePerformanceTest == null) {
                        throw new RuntimeException("[atomikos] pressure dataSource is null.");
                    }
                    return new AtomikosPressureConnection(dataSourcePerformanceTest, dataSourcePerformanceTest.getConnection(username, password), getUrl(dataSourcePerformanceTest), getUsername(dataSourcePerformanceTest), dbConnectionKey, dbType);
                }
            } catch (Throwable e) {
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0001")
                        .setMessage("数据源获取链接失败！" + ((Pradar.isClusterTest() ? "(压测流量)" : "") + ", url="
                                + (dataSourceBusiness == null ? null : getUrl(dataSourceBusiness))
                                + ", username=" + (dataSourceBusiness == null ? null : getUsername(dataSourceBusiness))))
                        .setDetail("get connection failed by dbMediatorDataSource, url="
                                + (dataSourceBusiness == null ? null : getUrl(dataSourceBusiness)) +
                                ", username=" + (dataSourceBusiness == null ? null : getUsername(dataSourceBusiness))
                                + "message: " + e.getMessage() + "\r\n" + printStackTrace(e));
//                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                error.report();
                throw new PressureMeasureError("get connection failed by dbMediatorDataSource. url="
                        + (dataSourceBusiness == null ? null : getUrl(dataSourceBusiness))
                        + ", username=" + (dataSourceBusiness == null ? null : getUsername(dataSourceBusiness)), e);
            }
        } else {
            final String url = getUrl(dataSourceBusiness);
            String dbType = JdbcUtils.getDbType(url, JdbcUtils.getDriverClassName(url));
            return new AtomikosBizConnection(dataSourceBusiness.getConnection(username, password), url, getUsername(dataSourceBusiness), dbType);
        }
    }

    @Override
    public void close() {
        if (dataSourcePerformanceTest != null) {
            try {
                dataSourcePerformanceTest.close();
            } catch (Throwable e) {
                LOGGER.error("[atomikos] close performance test datasource err!", e);
            }
        }
    }
}
