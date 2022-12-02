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

import com.pamirs.attach.plugin.common.datasource.WrappedDbMediatorDataSource;
import com.pamirs.attach.plugin.common.datasource.biz.BizConnection;
import com.pamirs.attach.plugin.common.datasource.normal.NormalConnection;
import com.pamirs.attach.plugin.common.datasource.pressure.PressureConnection;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.shulie.druid.util.JdbcUtils;
import org.logicalcobwebs.proxool.ProxoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @Auther: vernon
 * @Date: 2020/3/29 16:44
 * @Description:
 */
public class ProxoolMediaDataSource extends WrappedDbMediatorDataSource<ProxoolDataSource> implements DataSource {

    private final static Logger logger = LoggerFactory.getLogger(ProxoolMediaDataSource.class);

    @Override
    public String getUsername(ProxoolDataSource datasource) {
        String user = datasource.getUser();
        if (user == null) {
            try {
                Object definition = DataSourceWrapUtil.extractDefinition(datasource);
                Properties delegateProperties = ReflectionUtils.get(definition, "delegateProperties");
                user = delegateProperties.getProperty("user");
            } catch (Exception e) {
                logger.error("[proxool] fetch user property occur exception", e);
            }
        }
        return user;
    }

    @Override
    public String getUrl(ProxoolDataSource datasource) {
        String driverUrl = datasource.getDriverUrl();
        if (driverUrl == null) {
            try {
                Object definition = DataSourceWrapUtil.extractDefinition(datasource);
                driverUrl = ReflectionUtils.get(definition, "url");
            } catch (Exception e) {
                logger.error("[proxool] fetch driverUrl property occur exception", e);
            }
        }
        return driverUrl;
    }

    @Override
    public String getDriverClassName(ProxoolDataSource datasource) {
        String driver = datasource.getDriver();
        if (driver == null) {
            try {
                Object definition = DataSourceWrapUtil.extractDefinition(datasource);
                driver = ReflectionUtils.get(definition, "driver");
            } catch (Exception e) {
                logger.error("[proxool] fetch driver property occur exception", e);
            }
        }
        return driver;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (Pradar.isClusterTest()) {
            try {
                init();
                if (useTable) {
                    //影子表
                    if (dataSourceBusiness == null) {
                        throw new RuntimeException("Business dataSource is null.");
                    }
                    Connection hikariConnection = dataSourceBusiness.getConnection();
                    return new NormalConnection(dataSourceBusiness, hikariConnection, dbConnectionKey, url, username,
                            dbType, "proxool");
                } else {
                    if (dataSourcePerformanceTest == null) {
                        throw new RuntimeException("pressure dataSource is null.");
                    }
                    return new PressureConnection(dataSourcePerformanceTest, dataSourcePerformanceTest.getConnection(),
                            dataSourcePerformanceTest.getDriverUrl(), dataSourcePerformanceTest.getUser(), dbConnectionKey,
                            dbType);
                }
            } catch (Throwable e) {
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0001")
                        .setMessage("数据源获取链接失败！" + ((Pradar.isClusterTest() ? "(压测流量)" : "") + ", url="
                                + (dataSourceBusiness == null ? null : dataSourceBusiness.getDriverUrl())
                                + ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUser())))
                        .setDetail("get connection failed by dbMediatorDataSource, url="
                                + (dataSourceBusiness == null ? null : dataSourceBusiness.getDriverUrl()) +
                                ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUser())
                                + "message: " + e.getMessage() + "\r\n" + printStackTrace(e));
//                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                error.report();
                throw new PressureMeasureError("get connection failed by dbMediatorDataSource. url="
                        + (dataSourceBusiness == null ? null : dataSourceBusiness.getDriverUrl())
                        + ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUser()), e);
            }
        } else {
            String dbType = JdbcUtils.getDbType(dataSourceBusiness.getDriverUrl(),
                    JdbcUtils.getDriverClassName(dataSourceBusiness.getDriverUrl()));
            return new BizConnection(dataSourceBusiness.getConnection(), dataSourceBusiness.getDriverUrl(),
                    dataSourceBusiness.getUser(), dbType);
        }
    }

    @Override
    public void close() {
    }
}
