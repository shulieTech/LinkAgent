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
package com.pamirs.attach.plugin.common.datasource.trace.impl;

import com.pamirs.attach.plugin.common.datasource.trace.JdbcPradar;
import com.pamirs.attach.plugin.common.datasource.trace.SqlTraceMetaData;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fabing.zhaofb
 */
public class JdbcPradarImpl implements JdbcPradar {
    private final static Logger logger = LoggerFactory.getLogger(JdbcPradarImpl.class);

    @Override
    public void startRpc(SqlTraceMetaData sqlMetaData, String sql) {
        if (sqlMetaData == null) {
            return;
        }
        try {
            if (sql != null) {
                sqlMetaData.setSql(sql);
            }
            startRpc(sqlMetaData);
        } catch (Throwable e) {
            logger.error("Jdbc start rpc error. sql={}, sqlMetadata={}", sql, sqlMetaData, e);
        }
    }

    private void startRpc(SqlTraceMetaData sqlMetaData) {
        Pradar.startClientInvoke(sqlMetaData.getUrl(), sqlMetaData.getTableNames());
        Pradar.middlewareName(sqlMetaData.getDbType());
        Pradar.remoteIp(sqlMetaData.getHost());
        Pradar.remotePort(sqlMetaData.getPort());
        Object request = sqlMetaData.getParameters();
        if (!Pradar.isRequestOn()) {
            request = null;
        }
        Pradar.request(request);
    }


    @Override
    public void endRpc(SqlTraceMetaData sqlMetaData, Object result) {
        try {
            if (!Pradar.isResponseOn()) {
                result = null;
            }
            if (!Pradar.isClusterTest()) {
                Pradar.getInvokeContext().setIndex(sqlMetaData.getUrl());
            }
            Pradar.response(result);
            if (sqlMetaData != null) {
                Pradar.callBack(sqlMetaData.getSql());
            }
            if (result != null && result instanceof Exception) {
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED,
                        MiddlewareType.TYPE_DB);
            } else {
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS,
                        MiddlewareType.TYPE_DB);
            }
        } catch (Throwable e) {
            logger.error("Jdbc start rpc error. result={}, sqlMetadata={}", result, sqlMetaData, e);
        }
    }
}
