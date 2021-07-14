package com.pamirs.attach.plugin.common.datasource.trace;


public interface JdbcPradar {

    /**
     * execute之前写日志
     */
    void startRpc(SqlTraceMetaData sqlMetaData, String sql);

    /**
     * @param e
     */
    void endRpc(SqlTraceMetaData sqlMetaData, Object e);

}
