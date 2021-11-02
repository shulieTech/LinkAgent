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
package com.pamirs.pradar.pressurement.datasource;

import com.pamirs.pradar.pressurement.datasource.util.SqlMetaData;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * <p>{@code } instances should NOT be constructed in
 * standard programming. </p>
 *
 * <p>This constructor is public to permit tools that require a JavaBean
 * instance to operate.</p>
 */
@SuppressWarnings("all")
public abstract class DbMediatorDataSource<T> {

    /**
     * <p>{@code } instances should NOT be constructed in
     * standard programming. </p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
     */
    protected T dataSourceBusiness;//正式数据源
    /**
     * <p>{@code } instances should NOT be constructed in
     * standard programming. </p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
     */
    protected volatile T dataSourcePerformanceTest;//压测第二数据源


    //static Map<DataSource, String> map = new HashMap<DataSource, String>();

    /**
     * @return the dataSource
     */
    public T getDataSourceBusiness() {
        return dataSourceBusiness;
    }

    //use table or use database to do pt test
    public boolean useTable = false;

    //ip + ":" + port + "|" + dbName;
    public String dbConnectionKey = "";

    //db type support mysql or oracle
    /**
     * 数据库类型
     */
    protected String dbType = "";

    /**
     * 业务数据库类型
     */
    protected volatile String bizDbType = "";

    /**
     * url
     */
    protected String url;

    /**
     * 用户名
     */
    protected String username;

    protected SqlMetaData sqlMetaData;

    /**
     * @param dataSource the dataSource to set
     */
    public void setDataSourceBusiness(T dataSource) {
        this.dataSourceBusiness = dataSource;
    }

    /**
     * @return the dataSourcePerformanceTest
     */
    public T getDataSourcePerformanceTest() {
        return dataSourcePerformanceTest;
    }

    /**
     * @param dataSourcePerformanceTest the dataSourcePerformanceTest to set
     */
    public void setDataSourcePerformanceTest(T dataSourcePerformanceTest) {
        this.dataSourcePerformanceTest = dataSourcePerformanceTest;
    }

    public abstract Connection getConnection() throws SQLException;

    public abstract Connection getConnection(String username, String password) throws SQLException;

    public abstract <T> T unwrap(Class<T> iface) throws SQLException;

    public abstract boolean isWrapperFor(Class<?> iface) throws SQLException;

    public abstract PrintWriter getLogWriter() throws SQLException;

    public abstract void setLogWriter(PrintWriter out) throws SQLException;

    public abstract void setLoginTimeout(int seconds) throws SQLException;

    public abstract int getLoginTimeout() throws SQLException;

    public abstract Logger getParentLogger() throws Exception;

    public SqlMetaData getSqlMetaData() {
        return sqlMetaData;
    }

    public void setSqlMetaData(SqlMetaData sqlMetaData) {
        this.sqlMetaData = sqlMetaData;
    }

    /**
     * 关闭数据源
     */
    public abstract void close();
}
