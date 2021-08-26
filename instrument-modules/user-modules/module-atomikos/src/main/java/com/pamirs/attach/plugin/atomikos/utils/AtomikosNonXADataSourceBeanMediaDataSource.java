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
import com.pamirs.attach.plugin.atomikos.connection.AtomikosBizConnection;
import com.pamirs.attach.plugin.atomikos.connection.AtomikosNormalConnection;
import com.pamirs.attach.plugin.atomikos.connection.AtomikosPressureConnection;
import com.pamirs.attach.plugin.common.datasource.WrappedDbMediatorDataSource;
import com.pamirs.pradar.ConfigNames;
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

public class AtomikosNonXADataSourceBeanMediaDataSource extends WrappedDbMediatorDataSource<AtomikosNonXADataSourceBean> implements DataSource {

    private final static Logger logger = LoggerFactory.getLogger(AtomikosNonXADataSourceBeanMediaDataSource.class);


    @Override
    public String getUsername(AtomikosNonXADataSourceBean datasource) {
        return datasource.getUser();
    }

    @Override
    public String getUrl(AtomikosNonXADataSourceBean datasource) {
        return datasource.getUrl();
    }

    @Override
    public String getDriverClassName(AtomikosNonXADataSourceBean datasource) {
        return datasource.getDriverClassName();
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (Pradar.isClusterTest()) {
            try {
                init();
                if (useTable) {
                    //影子表
                    if (dataSourceBusiness == null) {
                        throw new RuntimeException("[atomikos] Business dataSource is null.");
                    }
                    Connection connection = dataSourceBusiness.getConnection();
                    return new AtomikosNormalConnection(dataSourceBusiness, connection, dbConnectionKey, url, username, dbType);
                } else {
                    if (dataSourcePerformanceTest == null) {
                        throw new RuntimeException("[atomikos] pressure dataSource is null.");
                    }
                    return new AtomikosPressureConnection(dataSourcePerformanceTest, dataSourcePerformanceTest.getConnection(), dataSourcePerformanceTest.getUrl(), dataSourcePerformanceTest.getUser(), dbConnectionKey, dbType);
                }
            } catch (Throwable e) {
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0001")
                        .setMessage("数据源获取链接失败！" + ((Pradar.isClusterTest() ? "(压测流量)" : "") + ", url="
                                + (dataSourceBusiness == null ? null : dataSourceBusiness.getUrl())
                                + ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUser())))
                        .setDetail("get connection failed by dbMediatorDataSource, url="
                                + (dataSourceBusiness == null ? null : dataSourceBusiness.getUrl()) +
                                ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUser())
                                + "message: " + e.getMessage() + "\r\n" + printStackTrace(e));
//                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                error.report();
                throw new PressureMeasureError("get connection failed by dbMediatorDataSource. url="
                        + (dataSourceBusiness == null ? null : dataSourceBusiness.getUrl())
                        + ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUser()), e);
            }
        } else {
            String dbType = JdbcUtils.getDbType(dataSourceBusiness.getUrl(), JdbcUtils.getDriverClassName(dataSourceBusiness.getUrl()));
            return new AtomikosBizConnection(dataSourceBusiness.getConnection(), dataSourceBusiness.getUrl(), dataSourceBusiness.getUser(), dbType);
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
                        throw new PressureMeasureError("[atomikos] Business dataSource is null.");
                    }
                    Connection connection = dataSourceBusiness.getConnection(username, password);
                    return new AtomikosNormalConnection(dataSourceBusiness, connection, dbConnectionKey, url, username, dbType);
                } else {
                    if (dataSourcePerformanceTest == null) {
                        throw new PressureMeasureError("[atomikos] pressure dataSource is null.");
                    }
                    return new AtomikosPressureConnection(dataSourcePerformanceTest, dataSourcePerformanceTest.getConnection(username, password),
                            dataSourcePerformanceTest.getUrl(), dataSourcePerformanceTest.getUser(), dbConnectionKey, dbType);
                }
            } catch (Throwable e) {
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0001")
                        .setMessage("获取dbcp数据库连接失败！")
                        .setDetail("[atomikos] 异常信息:" + e.getMessage());
                error.setMessage(error.getMessage() + "(压测流量)")
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                logger.error("[atomikos] pressure test flow get connection fail,{}", e.getMessage());
                error.report();
                throw new PressureMeasureError("[atomikos] pressure test flow get connection fail " + e.getMessage());
            }
        } else {
            String dbType = JdbcUtils.getDbType(dataSourceBusiness.getUrl(), JdbcUtils.getDriverClassName(dataSourceBusiness.getUrl()));
            return new AtomikosBizConnection(dataSourceBusiness.getConnection(username, password), dataSourceBusiness.getUrl(), dataSourceBusiness.getUser(), dbType);
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
