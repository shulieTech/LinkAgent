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
import com.pamirs.attach.plugin.neo4j.config.Neo4JSessionOperation;
import com.pamirs.attach.plugin.neo4j.destroy.Neo4jDestroy;
import com.pamirs.attach.plugin.neo4j.operation.Operation;
import com.pamirs.attach.plugin.neo4j.utils.DataSourceWrapUtil;
import com.pamirs.pradar.*;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowDataSourceConfigModifyEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.shared.service.DataSourceMeta;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.datasource.DbMediatorDataSource;
import com.pamirs.pradar.pressurement.datasource.util.DbUrlUtils;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.reflect.Reflect;
import org.apache.commons.lang.StringUtils;
import org.neo4j.ogm.authentication.AuthTokenCredentials;
import org.neo4j.ogm.authentication.Credentials;
import org.neo4j.ogm.authentication.UsernamePasswordCredentials;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.session.Neo4jSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName: Neo4jSessionOperationCutOffInterceptor
 * @author: wangjian
 * @Date: 2020/7/31 20:14
 * @Description:
 */
@Destroyable(Neo4jDestroy.class)
public class Neo4jSessionOperationCutOffInterceptor extends CutoffInterceptorAdaptor {

    private static Logger logger = LoggerFactory.getLogger(Neo4jSessionOperationCutOffInterceptor.class);

    public Neo4jSessionOperationCutOffInterceptor() {
        addListener();
    }

    @Override
    public CutOffResult cutoff0(Advice advice) {
        Object[] args = advice.getParameterArray();
        String methodName = advice.getBehaviorName();
        Object target = advice.getTarget();
        /**
         * 压测状态为关闭,如果当前为压测流量则直接报错
         */
        if (!PradarSwitcher.isClusterTestEnabled()) {
            if (Pradar.isClusterTest()) {
                throw new PressureMeasureError(PradarSwitcher.PRADAR_SWITCHER_OFF + ":" + AppNameUtils.appName());
            }
            return CutOffResult.passed();
        }
        if (!Pradar.isClusterTest()) {
            // 非压测流量
            return CutOffResult.passed();
        }
        if (target instanceof Neo4JSessionExt) {
            // 影子配置session回调
            return CutOffResult.passed();
        }

        // 避免因配置重新生效而导致使用旧配置
        Driver driver1 = Reflect.on(target).get(Neo4JConstants.DYNAMIC_FIELD_DRIVER);
        DriverConfiguration configuration = driver1.getConfiguration();
        String uri = configuration.getURI();
        Credentials credentials = configuration.getCredentials();
        String username = null;
        String password = null;
        if (credentials instanceof AuthTokenCredentials) {
            username = ((AuthTokenCredentials) credentials).credentials();
        } else if (credentials instanceof UsernamePasswordCredentials) {
            username = ((UsernamePasswordCredentials) credentials).getUsername();
            password = ((UsernamePasswordCredentials) credentials).getPassword();
        }
        DataSourceMeta<Neo4jSession> neo4jSessionDataSourceMeta = new DataSourceMeta<Neo4jSession>(uri, username, (Neo4jSession) target);
        DataSourceWrapUtil.wrap(neo4jSessionDataSourceMeta,password);
        // 压测流量转发影子库
        if (!DataSourceWrapUtil.pressureDataSources.containsKey(neo4jSessionDataSourceMeta) || null == DataSourceWrapUtil.pressureDataSources.get(neo4jSessionDataSourceMeta)) {
            ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0002")
                    .setMessage("没有配置对应的影子库！")
                    .setDetail("Neo4J")
                    .closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS)
                    .report();
            throw new PressureMeasureError("Neo4J-002:影子库配置不存在!");
        }
        DbMediatorDataSource<?> dbMediatorDataSource = DataSourceWrapUtil.pressureDataSources.get(neo4jSessionDataSourceMeta);
        Neo4JSessionExt dataSourcePerformanceTest = (Neo4JSessionExt) dbMediatorDataSource.getDataSourcePerformanceTest();
        Operation operation = Neo4JSessionOperation.of(methodName);
        PradarSwitcher.httpPassPrefix.set(dataSourcePerformanceTest.getDriver().getConfiguration().getURI());
        return CutOffResult.cutoff(operation.invoke(dataSourcePerformanceTest, args));
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
