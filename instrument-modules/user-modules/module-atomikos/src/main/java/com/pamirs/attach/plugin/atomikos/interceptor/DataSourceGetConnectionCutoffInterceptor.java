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
package com.pamirs.attach.plugin.atomikos.interceptor;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean;
import com.pamirs.attach.plugin.atomikos.destroy.AtomikosDestroy;
import com.pamirs.attach.plugin.atomikos.utils.AtomikosDataSourceBeanMediaDataSource;
import com.pamirs.attach.plugin.atomikos.utils.AtomikosDataSourceBeanWrapUtil;
import com.pamirs.attach.plugin.atomikos.utils.AtomikosNonXADataSourceBeanMediaDataSource;
import com.pamirs.attach.plugin.atomikos.utils.AtomikosNonXADataSourceBeanWrapUtil;
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
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/17 10:13 上午
 */
@Destroyable(AtomikosDestroy.class)
public class DataSourceGetConnectionCutoffInterceptor extends CutoffInterceptorAdaptor {
    private final static Logger logger = LoggerFactory.getLogger(DataSourceGetConnectionCutoffInterceptor.class.getName());

    public DataSourceGetConnectionCutoffInterceptor() {
        addListener();
    }

    public String getUsername(AtomikosDataSourceBean datasource) {
        String username = datasource.getXaProperties().getProperty("user");
        if (username == null) {
            username = datasource.getXaProperties().getProperty("username");
        }
        if (username == null) {
            username = datasource.getXaProperties().getProperty("User");
        }
        if (username == null) {
            username = datasource.getXaProperties().getProperty("USER");
        }
        return username;
    }

    public String getUrl(AtomikosDataSourceBean datasource) {
        String url = datasource.getXaProperties().getProperty("URL");
        if (url == null) {
            url = datasource.getXaProperties().getProperty("url");
        }
        return url;
    }

    private CutOffResult wrapAtomikosDataSourceBean(AtomikosDataSourceBean dataSourceBean) {
        DataSourceMeta<AtomikosDataSourceBean> dataSourceMeta = new DataSourceMeta<AtomikosDataSourceBean>(getUrl(dataSourceBean),
                getUsername(dataSourceBean), dataSourceBean);
        AtomikosDataSourceBeanWrapUtil.init(dataSourceMeta);

        Connection connection = null;

        /**
         * 所有的流量均切换到此逻辑上,防止业务有连接缓存后无法进入
         * 如果未找到配置情况下则当前流量为压测流量时返回null,非压测流量则执行业务连接池正常逻辑,此种情况可能由于数据源未配置的情况
         * 如果获取连接出错时如果流量为压测流量则返回null，非压测流量则执行业务连接池正常逻辑
         */
        if (AtomikosDataSourceBeanWrapUtil.pressureDataSources.containsKey(dataSourceMeta)) {
            AtomikosDataSourceBeanMediaDataSource mediatorDataSource = AtomikosDataSourceBeanWrapUtil.pressureDataSources.get(dataSourceMeta);
            if (mediatorDataSource != null) {
                try {
                    connection = mediatorDataSource.getConnection();
                } catch (SQLException e) {
                    throw new PressureMeasureError(e);
                }
            } else {
                if (!Pradar.isClusterTest()) {
                    return CutOffResult.passed();
                }
            }
            return CutOffResult.cutoff(connection);
        } else {
            if (!Pradar.isClusterTest()) {
                return CutOffResult.passed();
            }
            return CutOffResult.cutoff(null);
        }
    }

    private CutOffResult wrapAtomikosNonXADataSourceBean(AtomikosNonXADataSourceBean dataSourceBean) {
        DataSourceMeta<AtomikosNonXADataSourceBean> dataSourceMeta = new DataSourceMeta<AtomikosNonXADataSourceBean>(dataSourceBean.getUrl(), dataSourceBean.getUser(), dataSourceBean);
        AtomikosNonXADataSourceBeanWrapUtil.init(dataSourceMeta);

        Connection connection = null;

        /**
         * 所有的流量均切换到此逻辑上,防止业务有连接缓存后无法进入
         * 如果未找到配置情况下则当前流量为压测流量时返回null,非压测流量则执行业务连接池正常逻辑,此种情况可能由于数据源未配置的情况
         * 如果获取连接出错时如果流量为压测流量则返回null，非压测流量则执行业务连接池正常逻辑
         */
        if (AtomikosNonXADataSourceBeanWrapUtil.pressureDataSources.containsKey(dataSourceMeta)) {
            AtomikosNonXADataSourceBeanMediaDataSource mediatorDataSource = AtomikosNonXADataSourceBeanWrapUtil.pressureDataSources.get(dataSourceMeta);
            if (mediatorDataSource != null) {
                try {
                    connection = mediatorDataSource.getConnection();
                } catch (SQLException e) {
                    throw new PressureMeasureError(e);
                }
            } else {
                if (!Pradar.isClusterTest()) {
                    return CutOffResult.passed();
                }
            }
            return CutOffResult.cutoff(connection);
        } else {
            if (!Pradar.isClusterTest()) {
                return CutOffResult.passed();
            }
            return CutOffResult.cutoff(null);
        }
    }

    @Override
    public CutOffResult cutoff0(Advice advice) {
        Object target = advice.getTarget();
        ClusterTestUtils.validateClusterTest();

        try {
            if (target instanceof AtomikosDataSourceBean) {
                return wrapAtomikosDataSourceBean((AtomikosDataSourceBean) target);
            }
        } catch (Throwable e) {
            logger.error("[atomikos] wrap AtomikosDataSourceBean err!", e);
        }

        try {
            if (target instanceof AtomikosNonXADataSourceBean) {
                return wrapAtomikosNonXADataSourceBean((AtomikosNonXADataSourceBean) target);
            }
        } catch (Throwable e) {
            logger.error("[atomikos] wrap AtomikosNonXADataSourceBean err!", e);
        }

        logger.error("[atomikos] can't found any matched datasource type.");

        if (Pradar.isClusterTest()) {
            throw new PressureMeasureError("[atomikos] can't found any matched datasource type.");
        }
        return CutOffResult.passed();
    }

    private void addListener() {
        EventRouter.router().addListener(new PradarEventListener() {
            @Override
            public EventResult onEvent(IEvent event) {
                if (!(event instanceof ClusterTestSwitchOffEvent)) {
                    return EventResult.IGNORE;
                }
                //关闭压测数据源
                AtomikosDataSourceBeanWrapUtil.destroy();
                AtomikosNonXADataSourceBeanWrapUtil.destroy();
                return EventResult.success("tomcat-jdbc-plugin");
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
                    Iterator<Map.Entry<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource>> it = AtomikosDataSourceBeanWrapUtil.pressureDataSources.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<DataSourceMeta, AtomikosDataSourceBeanMediaDataSource> entry = it.next();
                        if (StringUtils.equalsIgnoreCase(DbUrlUtils.getKey(config.getUrl(), config.getUsername()),
                                DbUrlUtils.getKey(entry.getKey().getUrl(), entry.getKey().getUsername()))) {
                            AtomikosDataSourceBeanMediaDataSource value = entry.getValue();
                            it.remove();
                            try {
                                value.close();
                                if (logger.isInfoEnabled()) {
                                    logger.info("module-atomikos-datasource: destroyed shadow table datasource success. url:{} ,username:{}", entry.getKey().getUrl(), entry.getKey().getUsername());
                                }
                            } catch (Throwable e) {
                                logger.error("module-atomikos-datasource: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
                            }
                            break;
                        }
                    }

                    Iterator<Map.Entry<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource>> nonIt = AtomikosNonXADataSourceBeanWrapUtil.pressureDataSources.entrySet().iterator();
                    while (nonIt.hasNext()) {
                        Map.Entry<DataSourceMeta, AtomikosNonXADataSourceBeanMediaDataSource> entry = nonIt.next();
                        if (StringUtils.equalsIgnoreCase(config.getUrl(), entry.getKey().getUrl())
                                && StringUtils.equalsIgnoreCase(config.getUsername(), entry.getKey().getUsername())) {
                            AtomikosNonXADataSourceBeanMediaDataSource value = entry.getValue();
                            nonIt.remove();
                            try {
                                value.close();
                                if (logger.isInfoEnabled()) {
                                    logger.info("module-atomikos-datasource: destroyed shadow table datasource success. url:{} ,username:{}", entry.getKey().getUrl(), entry.getKey().getUsername());
                                }
                            } catch (Throwable e) {
                                logger.error("module-atomikos-datasource: closed datasource err! target:{}, url:{} username:{}", entry.getKey().getDataSource().hashCode(), entry.getKey().getUrl(), entry.getKey().getUsername(), e);
                            }
                            break;
                        }
                    }

                }
                return EventResult.success("module-atomikos-datasource: destroyed shadow table datasource success.");
            }

            @Override
            public int order() {
                return 2;
            }
        });
    }
}
