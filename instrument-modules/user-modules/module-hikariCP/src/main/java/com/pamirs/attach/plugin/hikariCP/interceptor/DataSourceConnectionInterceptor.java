/**
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pamirs.attach.plugin.hikariCP.interceptor;

import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.attach.plugin.dynamic.Type;
import com.pamirs.attach.plugin.dynamic.template.HikariTemplate;
import com.pamirs.attach.plugin.hikariCP.ListenerRegisterStatus;
import com.pamirs.attach.plugin.hikariCP.destroy.HikariCPDestroy;
import com.pamirs.attach.plugin.hikariCP.utils.DataSourceWrapUtil;
import com.pamirs.attach.plugin.hikariCP.utils.HikariMediaDataSource;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.ClusterTestUtils;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceConfigModifyEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.annotation.ListenerBehavior;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @Auther: vernon
 * @Date: 2020/3/29 13:01
 * @Description:
 */
@Destroyable(HikariCPDestroy.class)
@ListenerBehavior(isFilterBusinessData = true)
public class DataSourceConnectionInterceptor extends CutoffInterceptorAdaptor {
    private static Logger logger = LoggerFactory.getLogger(DataSourceConnectionInterceptor.class.getName());

    public DataSourceConnectionInterceptor() {
        addListener();
    }

    void attachment(Advice advice) {
        try {
            HikariDataSource target = (HikariDataSource) advice.getTarget();
            ResourceManager.set(new Attachment(
                            target.getJdbcUrl(),
                            "hikari",
                            Type.DataBaseType.types(),
                            new HikariTemplate()
                                    .setUrl(target.getJdbcUrl())
                                    .setUsername(target.getUsername())
                                    .setPassword(target.getPassword())
                                    .setDriverClassName(target.getDriverClassName())
                                    .setConnectionTimeout(target.getConnectionTimeout())
                                    .setConnectionTestQuery(target.getConnectionTestQuery())
                                    .setIdleTimeout(target.getIdleTimeout())
                                    .setMaxLifetime(target.getMaxLifetime())
                                    .setMinimumIdle(target.getIdleTimeout())
                                    .setMaximumPoolSize(target.getMaximumPoolSize())
                                    .setValidationTimeout(target.getValidationTimeout())
                    )
            );
        } catch (Throwable t) {
            logger.error(t.getMessage());
        }
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws SQLException {
        Object target = advice.getTarget();
        addListener();
        attachment(advice);
        ClusterTestUtils.validateClusterTest();
        HikariDataSource dataSource = (HikariDataSource) target;

        DataSourceMeta<HikariDataSource> dataSourceMeta = new DataSourceMeta<HikariDataSource>(dataSource.getJdbcUrl(), dataSource.getUsername(), dataSource);
        DataSourceWrapUtil.init(dataSourceMeta);

        Connection connection = null;
        /**
         * 所有的流量均切换到此逻辑上,防止业务有连接缓存后无法进入
         * 如果未找到配置情况下则当前流量为压测流量时返回null,非压测流量则执行业务连接池正常逻辑,此种情况可能由于数据源未配置的情况
         * 如果获取连接出错时如果流量为压测流量则返回null，非压测流量则执行业务连接池正常逻辑
         */
        HikariMediaDataSource mediatorDataSource = DataSourceWrapUtil.pressureDataSources.get(dataSourceMeta);
        if (mediatorDataSource != null) {
            try {
                connection = mediatorDataSource.getConnection();
            } catch (SQLException e) {
                if (Pradar.isClusterTest()) {
                    throw new PressureMeasureError(e);
                }
                throw e;
            }
            return CutOffResult.cutoff(connection);
        } else {
            if (!Pradar.isClusterTest()) {
                return CutOffResult.passed();
            }
            return CutOffResult.cutoff(null);
        }

    }

    private void addListener() {
        if (!ListenerRegisterStatus.getInstance().init()) {
            return;
        }
        EventRouter.router().addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (!(event instanceof ClusterTestSwitchOffEvent)) {
                    return EventResult.IGNORE;
                }
                //关闭压测数据源
                DataSourceWrapUtil.destroy();
                return EventResult.success("hikariCP-plugin");
            }

            @Override
            public int order() {
                return 8;
            }
        }).addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (!(event instanceof ShadowDataSourceConfigModifyEvent)) {
                    return EventResult.IGNORE;
                }
                ShadowDataSourceConfigModifyEvent shadowDataSourceConfigModifyEvent = (ShadowDataSourceConfigModifyEvent) event;
                Set<ShadowDatabaseConfig> target = shadowDataSourceConfigModifyEvent.getTarget();
                if (null == target || target.size() == 0) {
                    return EventResult.IGNORE;
                }
                for (ShadowDatabaseConfig config : target) {
                    Iterator<Map.Entry<DataSourceMeta, HikariMediaDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<DataSourceMeta, HikariMediaDataSource> entry = it.next();
                        if (StringUtils.equalsIgnoreCase(DbUrlUtils.getKey(config.getUrl(), config.getUsername()),
                                DbUrlUtils.getKey(entry.getKey().getUrl(), entry.getKey().getUsername()))) {
                            HikariMediaDataSource value = entry.getValue();
                            it.remove();
                            try {
                                value.close();
                                if (logger.isInfoEnabled()) {
                                    logger.info("module-hikariCP: destroyed shadow table datasource success. url:{} ,username:{}", entry.getKey().getUrl(), entry.getKey().getUsername());
                                }
                            } catch (Throwable e) {
                                logger.error("module-hikariCP: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
                            }
                            break;
                        }
                    }

                }
                return EventResult.success("module-hikariCP: destroyed shadow table datasource success.");
            }

            @Override
            public int order() {
                return 2;
            }
        });
    }
}
