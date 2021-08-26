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
package com.pamirs.attach.plugin.proxool.interceptor;

import com.pamirs.attach.plugin.proxool.ProxoolConst;
import com.pamirs.attach.plugin.proxool.destroy.ProxoolDestroy;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DatabaseUtils;
import com.pamirs.pradar.pressurement.datasource.SqlParser;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.druid.util.JdbcUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author angju
 * @date 2021/4/4 14:38
 */
@Destroyable(ProxoolDestroy.class)
public class ProxyStatementInvokeInterceptor extends ParametersWrapperInterceptorAdaptor {
    private final static Logger logger = LoggerFactory.getLogger(ProxyStatementInvokeInterceptor.class);

    private Method getUrlMethod;
    private Method getUserMethod;
    private Method getConnectionPoolMethod;
    private Method getDefinitionMethod;

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        if (!Pradar.isClusterTest() || args == null || args.length != 3) {
            return args;
        }
        if (!(args[2] instanceof Object[])) {
            return args;
        }
        Object[] methodArgs = (Object[]) args[2];
        if (methodArgs == null || methodArgs.length == 0) {
            return args;
        }
        if (!(methodArgs[0] instanceof String)) {
            return args;
        }

        initConnectionPropertiesMethod(advice.getTarget());
        Object connectionPool = Reflect.on(advice.getTarget()).call(getConnectionPoolMethod).get();
        Object connectionPoolDefinition = Reflect.on(connectionPool).call(getDefinitionMethod).get();
        String url = Reflect.on(connectionPoolDefinition).call(getUrlMethod).get();
        if (url != null) {
            final int index = url.indexOf('?');
            if (index != -1) {
                url = url.substring(0, index);
            }
        }
        String username = Reflect.on(connectionPoolDefinition).call(getUserMethod).get();
        String dbType = JdbcUtils.getDbType(url, JdbcUtils.getDriverClassName(url));
        boolean useTable = !DatabaseUtils.isShadowDatasource(url, username);
        String dbConnectionKey = DbUrlUtils.getKey(url, username);
        if (!GlobalConfig.getInstance().containsShadowDatabaseConfig(dbConnectionKey)) {
            dbConnectionKey = DbUrlUtils.getKey(url, null);
        }

        //因为获取的 url 和 username 有可能是影子的 url 和 username，需要是影子的 url 和 username 需要将是否影子表以及 dbConnectionKey 重新拿业务的 url 和 username计算
        if (!GlobalConfig.getInstance().containsShadowDatabaseConfig(dbConnectionKey)) {
            for (Map.Entry<String, ShadowDatabaseConfig> entry : GlobalConfig.getInstance().getShadowDatasourceConfigs().entrySet()) {
                if (StringUtils.equals(entry.getValue().getShadowUrl(), url) && StringUtils.equals(entry.getValue().getShadowUsername(), username)) {
                    String bizUrl = entry.getValue().getUrl();
                    String bizUser = entry.getValue().getUsername();
                    dbConnectionKey = DbUrlUtils.getKey(bizUrl, bizUser);
                    useTable = !DatabaseUtils.isShadowDatasource(bizUrl, bizUser);
                    break;
                }
            }
        }

        String sql = (String) methodArgs[0];
        String ptSql = null;
        if (useTable) {
            try {
                ptSql = SqlParser.replaceTable(sql, dbConnectionKey, dbType);
            } catch (SQLException e) {
                throw new PressureMeasureError(e.getMessage());
            }
        } else {
            ptSql = SqlParser.replaceSchema(sql, dbConnectionKey, dbType);
        }
        ((Object[]) args[2])[0] = ptSql;
        return args;
    }

    /**
     * 初始化获取连接属性的方法，包括获取 url、用户名、driver
     *
     * @param target
     * @throws NoSuchMethodException
     */
    private void initConnectionPropertiesMethod(Object target) throws NoSuchMethodException {
        if (getConnectionPoolMethod == null) {
            getConnectionPoolMethod = target.getClass().getSuperclass().getDeclaredMethod(ProxoolConst.REFLECT_GET_CONNECTION_POOL_METHOD);
            getConnectionPoolMethod.setAccessible(true);
        }

        Object connectionPool = null;
        if (getDefinitionMethod == null) {
            connectionPool = Reflect.on(target).call(getConnectionPoolMethod).get();
            getDefinitionMethod = connectionPool.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_DEFINITION_METHOD);
            getDefinitionMethod.setAccessible(true);
        }

        if (getUrlMethod == null || getUserMethod == null) {
            Object connectionPoolDefinition = Reflect.on(connectionPool).call(getDefinitionMethod).get();
            if (getUrlMethod == null) {
                getUrlMethod = connectionPoolDefinition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_URL_METHOD);
                getUrlMethod.setAccessible(true);
            }
            if (getUserMethod == null) {
                getUserMethod = connectionPoolDefinition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_USER_METHOD);
                getUserMethod.setAccessible(true);
            }
        }

    }
}
