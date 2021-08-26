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
package com.pamirs.attach.plugin.hikariCP.interceptor;

import com.pamirs.attach.plugin.hikariCP.destroy.HikariCPDestroy;
import com.pamirs.attach.plugin.hikariCP.utils.DataSourceWrapUtil;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @Auther: vernon
 * @Date: 2020/4/8 23:32
 * @Description:
 */
@Destroyable(HikariCPDestroy.class)
public class DataSourceConstructorInterceptor extends AroundInterceptor {

    @Override
    public void doAfter(Advice advice) {
        Object target = advice.getTarget();
        if (target instanceof HikariDataSource) {
            HikariDataSource dataSource = (HikariDataSource) target;
            DataSourceMeta dataSourceMeta = DataSourceMeta.build(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource);
            DataSourceWrapUtil.init(dataSourceMeta);
        }
    }
}
