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

package com.pamirs.attach.plugin.jdbc.trace.interceptor;

import com.pamirs.attach.plugin.common.datasource.trace.CheckedTracePreparedStatement;
import com.pamirs.attach.plugin.common.datasource.trace.PradarHelper;
import com.pamirs.attach.plugin.common.datasource.trace.SqlTraceMetaData;
import com.pamirs.attach.plugin.common.datasource.utils.ProxyFlag;
import com.pamirs.attach.plugin.jdbc.trace.JdbcTraceConstants;
import com.pamirs.attach.plugin.jdbc.trace.model.JdbcTraceBean;
import com.pamirs.attach.plugin.jdbc.trace.utils.JdbcTraceUtil;
import com.pamirs.pradar.MiddlewareType;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.ResultCode;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.sql.Statement;
import java.util.Map;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/31 14:33
 */
public class PreparedStatementInterceptor extends AroundInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(PreparedStatementInterceptor.class);

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    @Override
    public void doBefore(Advice advice) throws Throwable {
        try {
            if (ProxyFlag.inProxy() || isCheckedTracePreparedStatement(advice)) {
                return;
            }
            SqlTraceMetaData sqlMetaData = JdbcTraceUtil.buildMetaData((Statement) advice.getTarget());
            if (sqlMetaData == null) {
                logger.error("[JDBC-TRACE] PreparedStatement sqlTraceMetaData parse error, not record trace");
                return;
            }
            String sql = dynamicFieldManager.getDynamicField(advice.getTarget(), JdbcTraceConstants.sql);
            Map<Integer, Object> params = dynamicFieldManager.getDynamicField(advice.getTarget(),
                    JdbcTraceConstants.params);
            if (params != null) {
                for (Map.Entry<Integer, Object> item : params.entrySet()) {
                    SqlTraceMetaData.checkAndAddParameter(sqlMetaData, item.getKey(), item.getValue());
                }
            }

            boolean isStartSuccess = false;
            try {
                isStartSuccess = PradarHelper.startRpc(sqlMetaData, sql);
                JdbcTraceUtil.recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(), sqlMetaData.getParameters(), null, "executeFirst");
            } catch (Throwable e) {
                logger.error("[JDBC-TRACE] startRpc err!", e);
            }

            JdbcTraceBean traceBean = new JdbcTraceBean(sqlMetaData, isStartSuccess);
            advice.attach(traceBean);
        } finally {
            dynamicFieldManager.removeAll(advice.getTarget());
        }
    }

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (ProxyFlag.inProxy() || isCheckedTracePreparedStatement(advice)) {
            return;
        }
        JdbcTraceBean traceBean = advice.attachment();
        if (traceBean == null) {
            return;
        }

        try {
            if (traceBean.isStartSuccess()) {
                PradarHelper.endRpc(traceBean.getSqlTraceMetaData(), null);
            }
        } catch (Throwable e) {
            LOGGER.error("[JDBC-TRACE] endRpc err!", e);
            if (traceBean.isStartSuccess()) {
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_SUCCESS, MiddlewareType.TYPE_DB);
            }
        }
        JdbcTraceUtil.recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(),
                traceBean.getSqlTraceMetaData().getParameters(), null, "executeLast");
    }

    @Override
    public void doException(Advice advice) throws Throwable {
        if (ProxyFlag.inProxy() || isCheckedTracePreparedStatement(advice)) {
            return;
        }
        JdbcTraceBean traceBean = advice.attachment();
        if (traceBean == null) {
            return;
        }
        try {
            if (traceBean.isStartSuccess()) {
                PradarHelper.endRpc(traceBean.getSqlTraceMetaData(), advice.getThrowable());
            }
        } catch (Throwable e) {
            LOGGER.error("[JDBC-TRACE] endRpc err!", e);
            if (traceBean.isStartSuccess()) {
                Pradar.endClientInvoke(ResultCode.INVOKE_RESULT_FAILED, MiddlewareType.TYPE_DB);
            }
        }
        JdbcTraceUtil.recordDebugFlow(Pradar.getTraceId(), Pradar.getInvokeId(), Pradar.getLogType(),
                traceBean.getSqlTraceMetaData().getParameters(), advice.getThrowable(), "executeLast");
    }


    /**
     * 判断是否是 CheckedTracePreparedStatement
     *
     * @param advice Advice对象
     * @return true or false
     */
    private boolean isCheckedTracePreparedStatement(Advice advice) {
        return advice.getTarget() instanceof CheckedTracePreparedStatement;
    }
}
