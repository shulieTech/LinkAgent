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

import com.pamirs.attach.plugin.common.datasource.TraceConnection;
import com.pamirs.attach.plugin.common.datasource.biz.BizConnection;
import com.pamirs.attach.plugin.common.datasource.pressure.PressureConnection;
import com.pamirs.attach.plugin.proxool.ProxoolConst;
import com.pamirs.attach.plugin.proxool.destroy.ProxoolDestroy;
import com.pamirs.attach.plugin.proxool.utils.ConnectionPoolUtils;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ModificationInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.druid.util.JdbcUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.logicalcobwebs.proxool.ProxoolConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * @author angju
 * @date 2021/4/5 10:23
 */
@Destroyable(ProxoolDestroy.class)
public class ProxoolDriverConnectInterceptor extends ModificationInterceptorAdaptor {
    private static Logger logger = LoggerFactory.getLogger(ProxoolDriverConnectInterceptor.class.getName());

    private Method getDefinitionMethod;
    private Method getUrlMethod;
    private Method getUserMethod;
    private Method getDriverMethod;

    @Override
    public Object[] getParameter0(Advice advice) {
        if (!Pradar.isClusterTest()) {
            return advice.getParameterArray();
        }
        //proxool.ds
        String connectUrl = (String) advice.getParameterArray()[0];
        if (!connectUrl.startsWith("proxool")) {
            return advice.getParameterArray();
        }

        String alias = getAlias(connectUrl);
        //说明url有问题
        if (alias == null || alias.length() == 0) {
            return advice.getParameterArray();
        }

        Object[] args = advice.getParameterArray();
        if (ConnectionPoolUtils.getConnectionPoolMap().containsKey(Pradar.CLUSTER_TEST_PREFIX + alias)) {
            args[0] = "proxool." + Pradar.CLUSTER_TEST_PREFIX + alias;
            return args;
        }

        String username = null;
        String url = null;
        Properties definitionProperties = null;
        try {
            if (getDefinitionMethod == null) {
                getDefinitionMethod = ConnectionPoolUtils.getConnectionPoolMap().get(alias).getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_DEFINITION_METHOD);
                getDefinitionMethod.setAccessible(true);
            }
            Object definition = getDefinitionMethod.invoke(ConnectionPoolUtils.getConnectionPoolMap().get(alias));
            getUrlMethod = definition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_URL_METHOD);
            getUserMethod = definition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_USER_METHOD);
            Method getCompleteInfo = definition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_COMPLETE_INFO_METHOD);
            getUrlMethod.setAccessible(true);
            getUserMethod.setAccessible(true);
            getCompleteInfo.setAccessible(true);
            url = (String) getUrlMethod.invoke(definition);
            username = (String) getUserMethod.invoke(definition);
            definitionProperties = (Properties) getCompleteInfo.invoke(definition);
        } catch (Exception e) {
            logger.error("proxool init getDefinitionMethod and definition error.", e);
        }

        ShadowDatabaseConfig shadowDatabaseConfig = getShadowDatabaseConfig(url, username);
        if (shadowDatabaseConfig == null) {
            //未配置影子库表
            throw new PressureMeasureError("未配置影子库表: " + url + " " + username);
        }
        //影子表 直接返回
        if (shadowDatabaseConfig.isShadowTable()) {
            return args;
        }

        StringBuffer ptUrl = new StringBuffer();
        ptUrl.append("proxool");
        ptUrl.append(ProxoolConstants.ALIAS_DELIMITER);
        ptUrl.append(Pradar.CLUSTER_TEST_PREFIX + alias);
        ptUrl.append(ProxoolConstants.URL_DELIMITER);
        ptUrl.append(shadowDatabaseConfig.getShadowDriverClassName());
        ptUrl.append(ProxoolConstants.URL_DELIMITER);
        ptUrl.append(shadowDatabaseConfig.getShadowUrl());

        try {
            Class connectionPoolDefinitionClass = Thread.currentThread().getContextClassLoader().loadClass("org.logicalcobwebs.proxool.ConnectionPoolDefinition");
            Constructor connectionPoolDefinitionOfConstructor = connectionPoolDefinitionClass.getDeclaredConstructor(String.class, Properties.class, boolean.class);
            connectionPoolDefinitionOfConstructor.setAccessible(true);
            Object ptConnectionPoolDefinitionOfInstance = connectionPoolDefinitionOfConstructor.newInstance(ptUrl.toString(), buildPtProperties(definitionProperties, shadowDatabaseConfig), true);

            Class connectionPoolClass = Thread.currentThread().getContextClassLoader().loadClass("org.logicalcobwebs.proxool.ConnectionPool");
            Constructor connectionPoolOfConstructor = connectionPoolClass.getDeclaredConstructors()[0];
            connectionPoolOfConstructor.setAccessible(true);
            Object connectionPoolOfInstance = connectionPoolOfConstructor.newInstance(ptConnectionPoolDefinitionOfInstance);
            Method startMethod = connectionPoolClass.getDeclaredMethod("start");
            startMethod.setAccessible(true);
            startMethod.invoke(connectionPoolOfInstance);

            ConnectionPoolUtils.getConnectionPoolMap().put(Pradar.CLUSTER_TEST_PREFIX + alias, connectionPoolOfInstance);
            ConnectionPoolUtils.getConnectionPools().add(connectionPoolOfInstance);
        } catch (Exception e) {
            logger.error("proxool init ConnectionPool error.", e);
        }

        args[0] = "proxool." + Pradar.CLUSTER_TEST_PREFIX + alias;
        return args;
    }

    private Properties buildPtProperties(Properties bizProperties, ShadowDatabaseConfig shadowDatabaseConfig) {
        if (bizProperties == null) {
            Properties properties = new Properties();
            properties.put("user", shadowDatabaseConfig.getShadowUsername());
            properties.put("password", shadowDatabaseConfig.getShadowPassword());
            return properties;
        }

        Properties properties = new Properties();
        properties.putAll(bizProperties);
        //将配置的其他属性覆盖到已有的属性值上
        if (shadowDatabaseConfig.getProperties() != null) {
            for (Map.Entry<String, String> entry : shadowDatabaseConfig.getProperties().entrySet()) {
                if (properties.containsKey(entry.getKey()) && entry.getValue() != null) {
                    properties.put(entry.getKey(), entry.getValue());
                }
            }
        }
        //设置用户名密码
        properties.put("user", shadowDatabaseConfig.getShadowUsername());
        properties.put("password", shadowDatabaseConfig.getShadowPassword());

        return properties;
    }

    private String getAlias(String url) {
        String alias = null;
        final String prefix = ProxoolConstants.PROXOOL + ProxoolConstants.ALIAS_DELIMITER;

        // Check that the prefix is there
        if (url.startsWith(prefix)) {

            // Check for the alias
            int endOfPrefix = url.indexOf(ProxoolConstants.URL_DELIMITER);

            if (endOfPrefix > -1) {
                alias = url.substring(prefix.length(), endOfPrefix);
            } else {
                alias = url.substring(prefix.length());
            }
        }

        return alias;
    }

    private ShadowDatabaseConfig getShadowDatabaseConfig(String url, String username) {
        String key = DbUrlUtils.getKey(url, username);
        logger.info("bus datasource key1 is {}", key);
        for (Map.Entry<String, ShadowDatabaseConfig> entry : GlobalConfig.getInstance().getShadowDatasourceConfigs().entrySet()) {
            logger.info("bus datasource config key  is {}", entry.getKey());
        }
        ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
        if (shadowDatabaseConfig == null) {
            /**
             * 解决现在影子表配置没有username的问题,再尝试使用非用户名的判断一次
             */
            key = DbUrlUtils.getKey(url, null);
            logger.info("bus datasource key2 is {}", key);
        } else {
            return shadowDatabaseConfig;
        }

        shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(key);
        if (shadowDatabaseConfig == null) {
            return null;
        }
        return shadowDatabaseConfig;
    }

    @Override
    public Object getResult0(Advice advice) throws Throwable {
        Object returnObj = advice.getReturnObj();
        // 检查返回结果
        if (!(returnObj instanceof Connection) || returnObj instanceof BizConnection || returnObj instanceof PressureConnection) {
            return returnObj;
        }

        Connection conn = (Connection) returnObj;
        String connectUrl = (String) advice.getParameterArray()[0];
        if (!connectUrl.startsWith("proxool")) {
            return returnObj;
        }

        String alias = getAlias(connectUrl);
        //说明url有问题
        if (alias == null || alias.length() == 0) {
            return returnObj;
        }
        if (Pradar.isClusterTestPrefix(alias)) {
            return buildConnection(returnObj, conn, alias, true);
        }
        return buildConnection(returnObj, conn, alias, false);
    }

    /**
     * 构建业务连接,包装连接，支持 trace
     *
     * @param returnObj
     * @param conn
     * @param alias
     * @return
     * @throws NoSuchMethodException
     */
    private Object buildConnection(Object returnObj, Connection conn, String alias, boolean isPressureConnection) throws NoSuchMethodException {
        Object connectionPool = ConnectionPoolUtils.getConnectionPoolMap().get(alias);
        if (connectionPool == null) {
            return returnObj;
        }
        if (getDefinitionMethod == null) {
            getDefinitionMethod = connectionPool.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_DEFINITION_METHOD);
            getDefinitionMethod.setAccessible(true);
        }
        Object connectionPoolDefinition = Reflect.on(connectionPool).call(getDefinitionMethod).get();
        initConnectionPropertiesMethod(connectionPoolDefinition);
        String url = Reflect.on(connectionPoolDefinition).call(getUrlMethod).get();
        if (url != null) {
            final int index = url.indexOf('?');
            if (index != -1) {
                url = url.substring(0, index);
            }
        }
        String user = Reflect.on(connectionPoolDefinition).call(getUserMethod).get();
        String driver = Reflect.on(connectionPoolDefinition).call(getDriverMethod).get();
        return new TraceConnection(conn, url, user, isPressureConnection, JdbcUtils.getDbType(url, driver));
    }

    /**
     * 初始化获取连接属性的方法，包括获取 url、用户名、driver
     *
     * @param connectionPoolDefinition
     * @throws NoSuchMethodException
     */
    private void initConnectionPropertiesMethod(Object connectionPoolDefinition) throws NoSuchMethodException {
        if (getUrlMethod == null) {
            getUrlMethod = connectionPoolDefinition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_URL_METHOD);
            getUrlMethod.setAccessible(true);
        }
        if (getUserMethod == null) {
            getUserMethod = connectionPoolDefinition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_USER_METHOD);
            getUserMethod.setAccessible(true);
        }

        if (getDriverMethod == null) {
            getDriverMethod = connectionPoolDefinition.getClass().getDeclaredMethod(ProxoolConst.REFLECT_GET_DRIVER_METHOD);
            getDriverMethod.setAccessible(true);
        }
    }
}
