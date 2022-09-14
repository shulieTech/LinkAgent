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
package com.pamirs.attach.plugin.alibaba.druid.interceptor;

import com.alibaba.druid.pool.DruidAbstractDataSource;
import com.alibaba.druid.pool.DruidDataSource;
import com.pamirs.attach.plugin.alibaba.druid.destroy.DruidDestroy;
import com.pamirs.attach.plugin.alibaba.druid.listener.DruidShadowActiveEventListener;
import com.pamirs.attach.plugin.alibaba.druid.listener.DruidShadowDisableEventListener;
import com.pamirs.attach.plugin.alibaba.druid.obj.DbDruidMediatorDataSource;
import com.pamirs.attach.plugin.alibaba.druid.util.DataSourceWrapUtil;
import com.pamirs.attach.plugin.dynamic.Attachment;
import com.pamirs.attach.plugin.dynamic.ResourceManager;
import com.pamirs.attach.plugin.dynamic.Type;
import com.pamirs.attach.plugin.dynamic.template.DruidTemplate;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
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
import com.shulie.instrument.simulator.api.util.StringUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Destroyable(DruidDestroy.class)
@ListenerBehavior(isFilterBusinessData = true)
public class DruidInjectGetConnectionInterceptor extends CutoffInterceptorAdaptor {
    private static Logger logger = LoggerFactory.getLogger(DruidInjectGetConnectionInterceptor.class.getName());

    public DruidInjectGetConnectionInterceptor() {
        addListener();
    }

    void addAttachment(Advice advice) {
        try {
            DruidAbstractDataSource target = (DruidAbstractDataSource) advice.getTarget();
            ResourceManager.set(new Attachment(
                            target.getUrl(),
                            "alibaba-druid",
                            Type.DataBaseType.types(),
                            new DruidTemplate()
                                    .setUrl(target.getUrl())
                                    .setUsername(target.getUsername())
                                    .setPassword(target.getPassword())
                                    .setDriverClassName(target.getDriverClassName())
                                    .setMaxActive(target.getMaxActive())
                                    .setInitialSize(target.getInitialSize())
                                    .setRemoveAbandoned(target.isRemoveAbandoned())
                                    .setRemoveAbandonedTimeout(target.getRemoveAbandonedTimeoutMillis())
                                    .setTestWhileIdle(target.isTestWhileIdle())
                                    .setTestOnBorrow(target.isTestOnBorrow())
                                    .setTestOnReturn(target.isTestOnReturn())
                                    .setValidationQuery(defaultValidateQuery(target.getValidationQuery()))
                    )
            );
        } catch (Throwable t) {
            logger.error(Throwables.getStackTraceAsString(t));
        }
    }

    public String defaultValidateQuery(String validateQuery) {
        return StringUtil.isEmpty(validateQuery) ? "SELECT 1 FROM DUAL" : validateQuery;
    }

    @Override
    public CutOffResult cutoff0(Advice advice) throws SQLException {
        DruidDataSource target1 = (DruidDataSource) advice.getTarget();
        addAttachment(advice);
        DataSourceMeta<DruidDataSource> dataSourceMeta = new DataSourceMeta<DruidDataSource>(target1.getUrl(), target1.getUsername(), target1);
        ClusterTestUtils.validateClusterTest();
        DbDruidMediatorDataSource mediatorDataSource = DataSourceWrapUtil.doWrap(dataSourceMeta);
        //判断带有压测标示，是否初始化
        //初始化
        Connection connection = null;
        /**
         * 所有的流量均切换到此逻辑上,防止业务有连接缓存后无法进入
         * 如果未找到配置情况下则当前流量为压测流量时返回null,非压测流量则执行业务连接池正常逻辑,此种情况可能由于数据源未配置的情况
         * 如果获取连接出错时如果流量为压测流量则返回null，非压测流量则执行业务连接池正常逻辑
         */
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
        EventRouter.router().addListener(new PradarEventListener() {
                    @Override
                    public EventResult onEvent(IEvent event) {
                        if (!(event instanceof ClusterTestSwitchOffEvent)) {
                            return EventResult.IGNORE;
                        }
                        //关闭压测数据源
                        DataSourceWrapUtil.destroy();
                        return EventResult.success("alibaba-druid-datasource-plugin");
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
                            Iterator<Map.Entry<DataSourceMeta, DbDruidMediatorDataSource>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry<DataSourceMeta, DbDruidMediatorDataSource> entry = it.next();
                                if (StringUtils.equalsIgnoreCase(DbUrlUtils.getKey(config.getUrl(), config.getUsername()),
                                        DbUrlUtils.getKey(entry.getKey().getUrl(), entry.getKey().getUsername()))) {
                                    DbDruidMediatorDataSource value = entry.getValue();
                                    it.remove();
                                    try {
                                        value.close();
                                        if (logger.isInfoEnabled()) {
                                            logger.info("module-alibaba-druid: destroyed shadow{} datasource success. url:{} ,username:{}", config.isShadowTable() ? " table" : "", entry.getKey().getUrl(), entry.getKey().getUsername());
                                        }
                                    } catch (Throwable e) {
                                        logger.error("module-alibaba-druid: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
                                    }
                                    break;
                                }
                            }

                        }
                        return EventResult.success("module-alibaba-druid: destroyed shadow table datasource success.");
                    }

                    @Override
                    public int order() {
                        return 2;
                    }
                });
    }

}

