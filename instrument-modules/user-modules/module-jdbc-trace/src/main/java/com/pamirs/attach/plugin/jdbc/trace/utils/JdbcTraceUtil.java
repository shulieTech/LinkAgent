/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.jdbc.trace.utils;

import com.pamirs.attach.plugin.common.datasource.trace.SqlTraceMetaData;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.debug.DebugHelper;
import com.pamirs.pradar.json.ResultSerializer;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbType;
import com.pamirs.pradar.pressurement.datasource.util.SqlMetaData;
import com.shulie.druid.util.JdbcUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/31 14:41
 */
public class JdbcTraceUtil {

    private static final Logger logger = LoggerFactory.getLogger(JdbcTraceUtil.class);

    /**
     * 组装 sqlTraceMetaData对象
     *
     * @param statement Statement对象
     * @return SqlTraceMetaData
     */
    public static SqlTraceMetaData buildMetaData(Statement statement) {
        SqlTraceMetaData traceMetaData = null;
        try {
            SqlMetaData sqlMetaData = null;
            Connection connection = statement.getConnection();
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            String url = databaseMetaData.getURL();
            String userName = databaseMetaData.getUserName();
            String dbType = JdbcUtils.getDbType(url, databaseMetaData.getDriverName());
            String dbName = connection.getCatalog();

            DbType type = DbType.nameOf(dbType);
            if (type != null) {
                try {
                    sqlMetaData = type.sqlMetaData(url);
                } catch (Throwable e) {
                    logger.warn("[JDBC-TRACE] sqlMetaData fail", e);
                }
            }
            traceMetaData = new SqlTraceMetaData();
            traceMetaData.setUrl(url);
            traceMetaData.setUsername(userName);
            traceMetaData.setDbType(dbType);
            traceMetaData.setDbName(dbName);
            if (sqlMetaData != null) {
                traceMetaData.setHost(sqlMetaData.getHost());
                traceMetaData.setPort(sqlMetaData.getPort());
            }
        } catch (SQLException e) {
            logger.error("[JDBC-TRACE] statement getConnection error", e);
        }
        return traceMetaData;
    }

    /**
     * 序列化对象
     *
     * @param target 目标对象
     * @return 序列化后的数值
     */
    public static String serializeObject(Object target) {
        if (target == null) {
            return StringUtils.EMPTY;
        }
        try {
            return ResultSerializer.serializeObject(target, 2);
        } catch (Throwable e) {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 记录debug日志
     *
     * @param traceId   traceId
     * @param rpcId     rpcId
     * @param logType   logType
     * @param params    params
     * @param returnObj returnObj
     * @param method    method
     */
    public static void recordDebugFlow(final String traceId, final String rpcId, final Integer logType,
                                       final Object params,
                                       final Object returnObj, final String method) {
        if (!Pradar.isDebug()) {
            return;
        }

        String level;
        String pattern;
        Object return2log;
        String parameterArray = serializeObject(params);
        if (returnObj instanceof Throwable) {
            level = "ERROR";
            pattern = "%s, targetClass: %s, classLoader: %s, parameterArray: %s, throwable: %s";
            return2log = serializeObject(returnObj);
        } else {
            level = "INFO";
            pattern = "%s,targetClass: %s, classLoader: %s, parameterArray: %s, returnObj: %s";
            return2log = returnObj;
        }
        String content = String.format(pattern, method, SqlParser.class, SqlParser.class.getClassLoader().toString(),
                parameterArray, return2log);
        DebugHelper.addDebugInfo(level, content);
    }
}
