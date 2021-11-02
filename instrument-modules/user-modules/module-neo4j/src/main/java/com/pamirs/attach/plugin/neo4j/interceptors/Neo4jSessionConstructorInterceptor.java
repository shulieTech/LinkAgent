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
package com.pamirs.attach.plugin.neo4j.interceptors;

import com.pamirs.attach.plugin.neo4j.ListenerRegisterStatus;
import com.pamirs.attach.plugin.neo4j.Neo4JConstants;
import com.pamirs.attach.plugin.neo4j.config.Neo4JSessionExt;
import com.pamirs.attach.plugin.neo4j.destroy.Neo4jDestroy;
import com.pamirs.attach.plugin.neo4j.utils.DataSourceWrapUtil;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.PradarSwitcher;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceConfigModifyEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;
import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.authentication.AuthTokenCredentials;
import org.neo4j.ogm.authentication.Credentials;
import org.neo4j.ogm.authentication.UsernamePasswordCredentials;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.drivers.http.driver.HttpDriver;
import org.neo4j.ogm.session.Neo4jSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName: SessionFactoryOpenSessionInterceptor
 * @author: wangjian
 * @Date: 2020/7/31 11:15
 * @Description:
 */
@Destroyable(Neo4jDestroy.class)
public class Neo4jSessionConstructorInterceptor extends TraceInterceptorAdaptor {

    private Logger logger = LoggerFactory.getLogger(Neo4jSessionConstructorInterceptor.class);

    public Neo4jSessionConstructorInterceptor() {
        addListener();
    }

    @Override
    public void afterLast(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        /**
         * 压测状态判断
         * 影子session回调
         */
        if (!PradarSwitcher.isClusterTestEnabled() || target instanceof Neo4JSessionExt) {
            return;
        }
        try {
            // 业务库session
            Neo4jSession sourceSession = (Neo4jSession) target;
            DriverConfiguration driverConfiguration = ((HttpDriver) args[1]).getConfiguration();
            String s = driverConfiguration.getURI();
            Credentials credentials = driverConfiguration.getCredentials();
            String username = null;
            if (credentials instanceof AuthTokenCredentials) {
                username = ((AuthTokenCredentials) credentials).credentials();
            } else if (credentials instanceof UsernamePasswordCredentials) {
                username = ((UsernamePasswordCredentials) credentials).getUsername();
            }
            DataSourceMeta<Neo4jSession> neo4jSessionDataSourceMeta = new DataSourceMeta<Neo4jSession>(s, username, sourceSession);
            if (DataSourceWrapUtil.pressureDataSources.containsKey(neo4jSessionDataSourceMeta) && DataSourceWrapUtil.pressureDataSources.get(neo4jSessionDataSourceMeta) != null) {
                // 该业务库数据源已经初始化过影子库
                return;
            }
            if (isPerformanceDataSource(driverConfiguration)) {
                // 业务库配置是已初始化的影子库
                return;
            }
            // 从应用的影子库配置中获取
            String key = DbUrlUtils.getKey(neo4jSessionDataSourceMeta.getUrl(), neo4jSessionDataSourceMeta.getUsername());
            if (!GlobalConfig.getInstance().containsShadowDatabaseConfig(key)) {
                ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0002")
                        .setMessage("没有配置对应的影子表或影子库！")
                        .setDetail("业务库配置:::url: " + s)
                        .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                        .report();
                return;
            }
            MetaData metaData = (MetaData) args[0];
            DataSourceWrapUtil.metaDataMap.put(sourceSession, metaData);
            DataSourceWrapUtil.wrap(neo4jSessionDataSourceMeta);
        } catch (Throwable e) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0003")
                    .setMessage("影子库配置异常，无法由配置正确生成影子库！")
                    .setDetail("url: " + ((HttpDriver) args[1]).getConfiguration().getURI() + Throwables.getStackTraceAsString(e))
                    .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                    .report();
            throw new PressureMeasureError("Neo4J-002:影子库初始化失败:", e);
        }
    }

    /**
     * 是否是影子数据源
     *
     * @param sourceDriverConfig
     * @return
     */
    private boolean isPerformanceDataSource(DriverConfiguration sourceDriverConfig) {
        for (Map.Entry<DataSourceMeta, DbMediatorDataSource<Neo4jSession>> entry : DataSourceWrapUtil.pressureDataSources.entrySet()) {
            DbMediatorDataSource<Neo4jSession> mediatorDataSource = entry.getValue();
            if (mediatorDataSource.getDataSourcePerformanceTest() != null
                    && compareToShadow(sourceDriverConfig, mediatorDataSource)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareToShadow(DriverConfiguration sourceDriverConfig, DbMediatorDataSource<?> mediatorDataSource) {
        Neo4JSessionExt dataSourcePerformanceTest = (Neo4JSessionExt) mediatorDataSource.getDataSourcePerformanceTest();
        Driver driver = dataSourcePerformanceTest.getDriver();
        DriverConfiguration configuration = driver.getConfiguration();
        return StringUtils.equals(sourceDriverConfig.getURI(), configuration.getURI());
    }

    @Override
    public String getPluginName() {
        return Neo4JConstants.PLUGIN_NAME;
    }

    @Override
    public int getPluginType() {
        return Neo4JConstants.PLUGIN_TYPE;
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
                    Iterator<Map.Entry<DataSourceMeta, DbMediatorDataSource<Neo4jSession>>> it = DataSourceWrapUtil.pressureDataSources.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<DataSourceMeta, DbMediatorDataSource<Neo4jSession>> entry = it.next();
                        if (StringUtils.equalsIgnoreCase(DbUrlUtils.getKey(config.getUrl(), config.getUsername()),
                                DbUrlUtils.getKey(entry.getKey().getUrl(), entry.getKey().getUsername()))) {
                            DbMediatorDataSource<Neo4jSession> value = entry.getValue();
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
