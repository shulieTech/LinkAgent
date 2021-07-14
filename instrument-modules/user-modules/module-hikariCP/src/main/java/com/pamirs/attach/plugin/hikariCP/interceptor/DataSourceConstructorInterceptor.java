package com.pamirs.attach.plugin.hikariCP.interceptor;

import com.pamirs.attach.plugin.hikariCP.utils.DataSourceWrapUtil;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.zaxxer.hikari.HikariDataSource;

/**
 * @Auther: vernon
 * @Date: 2020/4/8 23:32
 * @Description:
 */
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
