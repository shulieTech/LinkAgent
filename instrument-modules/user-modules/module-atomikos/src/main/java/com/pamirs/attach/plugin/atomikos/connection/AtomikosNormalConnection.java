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
package com.pamirs.attach.plugin.atomikos.connection;

import com.atomikos.util.DynamicProxy;
import com.pamirs.attach.plugin.common.datasource.normal.NormalConnection;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2020/11/27 3:34 下午
 */
public class AtomikosNormalConnection extends NormalConnection implements DynamicProxy {

    /**
     * <p>{@code } instances should NOT be constructed in
     * standard programming. </p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean
     * instance to operate.</p>
     *
     * @param dataSource
     * @param connection
     * @param dbConnectionKey
     * @param dbType
     */
    public AtomikosNormalConnection(DataSource dataSource, Connection connection, String dbConnectionKey, String url, String username, String dbType) {
        super(dataSource, connection, dbConnectionKey, url, username, dbType);
    }

    @Override
    public Object getInvocationHandler() {
        if (connection instanceof DynamicProxy) {
            return ((DynamicProxy) connection).getInvocationHandler();
        }
        return null;
    }
}
