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
package com.pamirs.attach.plugin.hikariCP.utils;

import com.pamirs.attach.plugin.common.datasource.WrappedDbMediatorDataSource;
import com.pamirs.attach.plugin.common.datasource.biz.BizConnection;
import com.pamirs.attach.plugin.common.datasource.normal.NormalConnection;
import com.pamirs.attach.plugin.common.datasource.pressure.PressureConnection;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.shulie.druid.util.JdbcUtils;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @Auther: vernon
 * @Date: 2020/3/29 16:44
 * @Description:
 */
public class HikariMediaDataSource extends WrappedDbMediatorDataSource<HikariDataSource> implements DataSource {

    private final static Logger logger = LoggerFactory.getLogger(HikariMediaDataSource.class);


    @Override
    public String getUsername(HikariDataSource datasource) {
        return datasource.getUsername();
    }

    @Override
    public String getUrl(HikariDataSource datasource) {
        return datasource.getJdbcUrl();
    }

    @Override
    public String getDriverClassName(HikariDataSource datasource) {
        // 因为低版本Hikaricp没有getDriverClassName方法 所以通过反射获取
        // 因为这个方法只会init的时候调用一次所以不需要考虑性能问题
        return Reflect.on(datasource).get("driverClassName");
    }

    @Override
    protected String getMidType() {
        return "Hikari";
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
                    return new NormalConnection(dataSourceBusiness, hikariConnection, dbConnectionKey, url, username, dbType,
                            getMidType());
                } else {
                    if (dataSourcePerformanceTest == null) {
                        DataSourceWrapUtil.retryInitPerformanceTest(this);
                    }
                    if (dataSourcePerformanceTest == null) {
                        throw new RuntimeException("pressure dataSource is null.");
                    }
                    return new PressureConnection(dataSourcePerformanceTest, dataSourcePerformanceTest.getConnection(),
                            dataSourcePerformanceTest.getJdbcUrl(), dataSourcePerformanceTest.getUsername(), dbConnectionKey, dbType);
                }
            } catch (Throwable e) {
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0001")
                        .setMessage("数据源获取链接失败！" + ((Pradar.isClusterTest() ? "(压测流量)" : "") + ", url="
                                + (dataSourceBusiness == null ? null : dataSourceBusiness.getJdbcUrl())
                                + ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUsername())))
                        .setDetail("get connection failed by dbMediatorDataSource, url="
                                + (dataSourceBusiness == null ? null : dataSourceBusiness.getJdbcUrl()) +
                                ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUsername())
                                + "message: " + e.getMessage() + "\r\n" + printStackTrace(e));
//                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                error.report();
                throw new PressureMeasureError("get connection failed by dbMediatorDataSource. url="
                        + (dataSourceBusiness == null ? null : dataSourceBusiness.getJdbcUrl())
                        + ", username=" + (dataSourceBusiness == null ? null : dataSourceBusiness.getUsername()), e);
            }
        } else {
            String dbType = JdbcUtils.getDbType(dataSourceBusiness.getJdbcUrl(), JdbcUtils.getDriverClassName(dataSourceBusiness.getJdbcUrl()));
            return new BizConnection(dataSourceBusiness.getConnection(), dataSourceBusiness.getJdbcUrl(), dataSourceBusiness.getUsername(), dbType);
        }

    }

    @Override
    public void close() {
        if (dataSourcePerformanceTest != null) {
            try {
                dataSourcePerformanceTest.close();
            } catch (Throwable e) {
                LOGGER.error("[hikari] close performance test datasource err!", e);
            }
        }
    }
}
