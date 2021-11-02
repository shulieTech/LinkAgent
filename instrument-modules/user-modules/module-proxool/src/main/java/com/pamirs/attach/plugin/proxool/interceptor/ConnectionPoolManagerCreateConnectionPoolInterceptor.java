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

import com.pamirs.attach.plugin.proxool.destroy.ProxoolDestroy;
import com.pamirs.attach.plugin.proxool.utils.ConnectionPoolUtils;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * @author angju
 * @date 2021/4/4 22:41
 */
@Destroyable(ProxoolDestroy.class)
public class ConnectionPoolManagerCreateConnectionPoolInterceptor extends CutoffInterceptorAdaptor {
    private final static Logger logger = LoggerFactory.getLogger(ConnectionPoolManagerCreateConnectionPoolInterceptor.class);
    private final static boolean isInfoEnabled = logger.isInfoEnabled();

    private String url = null;
    private String username = null;

    private boolean init = false;

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (init) {
            return CutOffResult.passed();
        }
        Object connectionPoolManager = advice.getTarget();
        Object connectionPoolDefinition = advice.getParameterArray()[0];
        setUrlAndUsername(connectionPoolDefinition);
        try {
            Field connectionPoolsField = connectionPoolManager.getClass().getDeclaredField("connectionPools");
            connectionPoolsField.setAccessible(true);
            Set connectionPools = (Set) connectionPoolsField.get(advice.getTarget());

            Field connectionPoolMapField = connectionPoolManager.getClass().getDeclaredField("connectionPoolMap");
            connectionPoolMapField.setAccessible(true);
            Map connectionPoolMap = (Map) connectionPoolMapField.get(advice.getTarget());

            ConnectionPoolUtils.setConnectionPools(connectionPools);
            ConnectionPoolUtils.setConnectionPoolMap(connectionPoolMap);


        } catch (Throwable e) {

        }

        init = true;
        return CutOffResult.passed();
    }

    private void setUrlAndUsername(Object target) {
        if (!StringUtils.isEmpty(url) && !StringUtils.isEmpty(username)) {
            return;
        }
        Method getUrlMethod = null;
        Method getUserMethod = null;
        try {

            getUrlMethod = target.getClass().getDeclaredMethod("getUrl");
            getUserMethod = target.getClass().getDeclaredMethod("getUser");
            getUrlMethod.setAccessible(true);
            getUserMethod.setAccessible(true);
            url = (String) getUrlMethod.invoke(target);
            username = (String) getUserMethod.invoke(target);

        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } finally {
            if (getUrlMethod != null) {
                getUrlMethod.setAccessible(true);
            }
            if (getUserMethod != null) {
                getUserMethod.setAccessible(true);
            }
        }
    }
}
