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
package com.pamirs.attach.plugin.common.datasource.trace;

import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.debug.DebugHelper;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.InterceptorInvokerHelper;
import com.pamirs.pradar.json.ResultSerializer;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.SqlMetaData;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * 带压测检测功能的可追踪的 Statement
 *
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/12/22 10:26 上午
 */
public class CheckedTraceStatement implements Statement {
    protected final static Logger LOGGER = LoggerFactory.getLogger(CheckedTraceStatement.class);
    protected Statement targetStatement;
    protected SqlTraceMetaData sqlMetaData;
    protected boolean isClosed;
    protected boolean isPressureConnection;

    private static String serializeObject(Object target) {
        if (target == null) {
            return StringUtils.EMPTY;
        }
        try {
            return ResultSerializer.serializeObject(target, 2);
        } catch (Throwable e) {
            return StringUtils.EMPTY;
        }
    }

    protected void recordDebugFlow(final String traceId, final String rpcId, final Integer logType, final Object params, final Object returnObj, final String method) {
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

    private void check() {
        if (Pradar.isClusterTest()) {
            if (!isPressureConnection) {
                PressureMeasureError error = new PressureMeasureError("get a pressurement request in business connection.");
                if (this instanceof CheckedTraceCallableStatement) {
                    recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), error, "prepareCall");
                } else if (this instanceof CheckedTracePreparedStatement) {
                    recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), error, "preparedStatement");
                } else {
                    recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), error, "createStatement");
                }
                throw error;
            }
        } else {
            if (isPressureConnection) {
                PressureMeasureError error = new PressureMeasureError("get a biz request in pressure connection.");
                if (this instanceof CheckedTraceCallableStatement) {
                    recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), error, "prepareCall");
                } else if (this instanceof CheckedTracePreparedStatement) {
                    recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), error, "preparedStatement");
                } else {
                    recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), error, "createStatement");
                }
                throw error;
            }
        }
    }

    public CheckedTraceStatement(Statement statement, String url, String username, String dbType, boolean isPressureConnection, SqlMetaData sqlMetaData) {
        this(statement, url, username, dbType, isPressureConnection, true, sqlMetaData);
    }

    public CheckedTraceStatement(Statement statement, String url, String username, String dbType, boolean isPressureConnection, boolean isChecked, SqlMetaData sqlMetaData) {
        this.targetStatement = statement;
        this.sqlMetaData = new SqlTraceMetaData();
        this.sqlMetaData.setUrl(url);
        this.sqlMetaData.setUsername(username);
        this.sqlMetaData.setDbType(dbType);
        this.isPressureConnection = isPressureConnection;
        try {
            if (sqlMetaData != null) {
                this.sqlMetaData.setDbName(sqlMetaData.getDbName());
                this.sqlMetaData.setHost(sqlMetaData.getHost());
                this.sqlMetaData.setPort(sqlMetaData.getPort());
            }
        } catch (Throwable e) {
        }
        if (isChecked) {
            check();
        }
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        this.targetStatement.addBatch(sql);
        try {
            sqlMetaData.setSql(sql);
        } catch (Throwable e) {
            InterceptorInvokerHelper.handleException(e);
        }
    }

    @Override
    public void cancel() throws SQLException {
        this.targetStatement.cancel();
    }

    @Override
    public void clearBatch() throws SQLException {
        this.targetStatement.clearBatch();
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.targetStatement.clearWarnings();
    }

    @Override
    public void close() throws SQLException {
        this.targetStatement.close();
        this.sqlMetaData = null;
        this.isClosed = true;
    }

    private boolean isPradarStatement() {
        try {
            return targetStatement instanceof CheckedTraceStatement;
        } catch (Throwable e) {
            return true;
        }
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).execute(sql);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            boolean result = targetStatement.execute(sql);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeLast");
        }
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).execute(sql, autoGeneratedKeys);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            boolean result = targetStatement.execute(sql, autoGeneratedKeys);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeLast");
        }
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).execute(sql, columnIndexes);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            boolean result = targetStatement.execute(sql, columnIndexes);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeLast");
        }
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).execute(sql, columnNames);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            boolean result = targetStatement.execute(sql, columnNames);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeLast");
        }
    }

    @Override
    public int[] executeBatch() throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).executeBatch();
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            int[] result = targetStatement.executeBatch();
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeBatchLast");
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).executeQuery(sql);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            ResultSet result = targetStatement.executeQuery(sql);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeQueryLast");
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).executeUpdate(sql);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            int result = targetStatement.executeUpdate(sql);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeUpdateLast");
        }
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).executeUpdate(sql, autoGeneratedKeys);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            int result = targetStatement.executeUpdate(sql, autoGeneratedKeys);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeUpdateLast");
        }
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).executeUpdate(sql, columnIndexes);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            int result = targetStatement.executeUpdate(sql, columnIndexes);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeUpdateLast");
        }
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        if (isPradarStatement()) {
            return (targetStatement).executeUpdate(sql, columnNames);
        }
        boolean isStartSuccess = false;
        String rpcId = null;
        String traceId = null;
        Integer logType = null;
        try {
            isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
            traceId = Pradar.getTraceId();
            rpcId = Pradar.getInvokeId();
            logType = Pradar.getLogType();
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), null, "executeFirst");
        } catch (Throwable e) {
            LOGGER.error("[jdbc] startRpc err!", e);
        }
        boolean isException = false;
        Object ex = null;
        try {
            int result = targetStatement.executeUpdate(sql, columnNames);
            ex = true;
            return result;
        } catch (SQLException e) {
            ex = e;
            isException = true;
            throw e;
        } catch (Throwable e) {
            ex = e;
            isException = true;
            throw new SQLException(e);
        } finally {
            try {
                if (isStartSuccess) {
                    PradarHelper.endRpc(sqlMetaData, ex);
                }
            } catch (Throwable e) {
                LOGGER.error("[jdbc] endRpc err!", e);
                if (isStartSuccess) {
                    Pradar.endClientInvoke(isException ? ResultCode.INVOKE_RESULT_FAILED : ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
                }
            }
            recordDebugFlow(traceId, rpcId, logType, sqlMetaData.getParameters(), ex, "executeUpdateLast");
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.targetStatement.getConnection();
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return this.targetStatement.getFetchDirection();
    }

    @Override
    public int getFetchSize() throws SQLException {
        return this.targetStatement.getFetchSize();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return this.targetStatement.getGeneratedKeys();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return this.targetStatement.getMaxFieldSize();
    }

    @Override
    public int getMaxRows() throws SQLException {
        return this.targetStatement.getMaxRows();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return this.targetStatement.getMoreResults();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return this.targetStatement.getMoreResults(current);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return this.targetStatement.getQueryTimeout();
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return this.targetStatement.getResultSet();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return this.targetStatement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return this.targetStatement.getResultSetHoldability();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return this.targetStatement.getResultSetType();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return this.targetStatement.getUpdateCount();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return this.targetStatement.getWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        this.targetStatement.setCursorName(name);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        this.targetStatement.setEscapeProcessing(enable);
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        this.targetStatement.setFetchDirection(direction);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.targetStatement.setFetchSize(rows);
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        this.targetStatement.setMaxFieldSize(max);
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        this.targetStatement.setMaxRows(max);
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.targetStatement.setQueryTimeout(seconds);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        try {
            return iface.isAssignableFrom(this.getClass());
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        try {
            return targetStatement.unwrap(iface);
        } catch (Throwable e) {
            return (T) this;
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        try {
            return this.targetStatement.isClosed();
        } catch (AbstractMethodError e) {
        } catch (NoSuchMethodError e) {
        }
        return this.isClosed;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        try {
            this.targetStatement.setPoolable(poolable);
        } catch (AbstractMethodError e) {
        } catch (NoSuchMethodError e) {
        }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        try {
            return this.targetStatement.isPoolable();
        } catch (AbstractMethodError e) {
        } catch (NoSuchMethodError e) {
        }
        return false;
    }

    public void closeOnCompletion() throws SQLException {
        try {
            this.targetStatement.closeOnCompletion();
        } catch (AbstractMethodError e) {
        } catch (NoSuchMethodError e) {
        }
    }

    public boolean isCloseOnCompletion() throws SQLException {
        try {
            return this.targetStatement.isCloseOnCompletion();
        } catch (AbstractMethodError e) {
        } catch (NoSuchMethodError e) {
        }
        return false;
    }
}
