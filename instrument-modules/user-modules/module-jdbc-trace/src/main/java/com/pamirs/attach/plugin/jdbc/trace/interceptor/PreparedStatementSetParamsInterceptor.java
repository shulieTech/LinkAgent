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
import com.pamirs.attach.plugin.common.datasource.utils.ProxyFlag;
import com.pamirs.attach.plugin.jdbc.trace.JdbcTraceConstants;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/8/31 15:05
 */
public class PreparedStatementSetParamsInterceptor extends AroundInterceptor {

    private final static Map<String, Object> methodValueMap = new HashMap<String, Object>();

    static {
        methodValueMap.put("setBinaryStream", "BinaryStream");
        methodValueMap.put("setBlob", "Blob");
        methodValueMap.put("setCharacterStream", "CharacterStream");
        methodValueMap.put("setClob", "Clob");
        methodValueMap.put("setNull", null);
        methodValueMap.put("setRef", "Ref");
        methodValueMap.put("setUnicodeStream", "UnicodeStream");
        methodValueMap.put("setRowId", "RowId");
        methodValueMap.put("setNCharacterStream", "NCharacterStream");
        methodValueMap.put("setNClob", "NClob");
        methodValueMap.put("setSQLXML", "SQLXML");
        methodValueMap.put("setAsciiStream", "AsciiStream");
    }

    @Resource
    private DynamicFieldManager dynamicFieldManager;

    @Override
    public void doAfter(Advice advice) throws Throwable {
        if (ProxyFlag.inProxy() || advice.getTarget() instanceof CheckedTracePreparedStatement) {
            return;
        }
        Object[] methodArgs = advice.getParameterArray();
        if (methodArgs.length < 2 || !int.class.isInstance(methodArgs[0])) {
            return;
        }
        Map<Integer, Object> params = dynamicFieldManager.getDynamicField(advice.getTarget(), JdbcTraceConstants.params);
        if (params == null) {
            params = new HashMap<Integer, Object>();
            dynamicFieldManager.setDynamicField(advice.getTarget(), JdbcTraceConstants.params, params);
        }
        Object value = null;
        if (methodValueMap.containsKey(advice.getBehaviorName())) {
            value = methodValueMap.get(advice.getBehaviorName());
        } else {
            value = methodArgs[1];
        }
        params.put((Integer) methodArgs[0], value);
    }
}
