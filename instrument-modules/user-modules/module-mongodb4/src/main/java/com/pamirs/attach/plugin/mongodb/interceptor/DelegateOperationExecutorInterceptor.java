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
package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.client.internal.OperationExecutor;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.internal.connection.Cluster;
import com.mongodb.internal.operation.ReadOperation;
import com.mongodb.internal.operation.WriteOperation;
import com.pamirs.attach.plugin.mongodb.utils.Caches;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessor;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessorFactory;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.resource.DynamicFieldManager;
import com.shulie.instrument.simulator.api.util.ReflectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author angju
 * @date 2021/3/31 21:19
 */
public class DelegateOperationExecutorInterceptor extends CutoffInterceptorAdaptor {

    @Resource
    protected DynamicFieldManager manager;

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        //影子库重入
        if (manager.getDynamicField(advice.getTarget(), "isCluster") != null){
            return CutOffResult.passed();
        }
        Object[] args = advice.getParameterArray();

        Class operationClass = args[0].getClass();
        Object operation = args[0];
        OperationAccessor operationAccessor = OperationAccessorFactory.getOperationAccessor(operationClass);
        if (operationAccessor == null) {
            LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
            throw new PressureMeasureError("mongo not support pressure operation class is " + args[0].getClass().getName());
        }

        MongoClientDelegate mongoClientDelegate = Caches.getMongoClientDelegate(advice.getTarget());

        ShadowDatabaseConfig shadowDatabaseConfig = getShadowDatabaseConfig(mongoClientDelegate);
        if (shadowDatabaseConfig == null) {
            if (operationAccessor.isRead()) {
                return CutOffResult.passed();
            } else {
                MongoNamespace busMongoNamespace = operationAccessor.getMongoNamespace(operation);
                ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongodb 不存在对应影子表或影子库:" + busMongoNamespace.getFullName())
                        .setDetail("mongodb "+StringUtils.join(((Cluster)(ReflectionUtils.getFieldValue(mongoClientDelegate,"cluster"))).getSettings().getHosts(), ",")+"不存在对应影子表或影子库:" + busMongoNamespace.getFullName());
                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                error.report();
                throw new PressureMeasureError("mongo 对应影子表或影子库:" + busMongoNamespace.getFullName());
            }
        }

        if (shadowDatabaseConfig.isShadowTable()) {
            doShadowTable(operation, operationAccessor, shadowDatabaseConfig);
            return CutOffResult.passed();
        } else {
            return CutOffResult.cutoff(doShadowDb(args, operationAccessor, shadowDatabaseConfig));
        }
    }

    private Object doShadowDb(Object[] args, OperationAccessor operationAccessor,
        ShadowDatabaseConfig shadowDatabaseConfig) throws Exception {
        Object operation = args[0];
        MongoNamespace busMongoNamespace = operationAccessor.getMongoNamespace(operation);
        OperationExecutor ptExecutor = Caches.getPtOperationExecutor(operationAccessor, shadowDatabaseConfig,
            operation, busMongoNamespace);
        if (manager.getDynamicField(ptExecutor, "isCluster") == null){
            manager.setDynamicField(ptExecutor, "isCluster", true);
        }
        if (operation instanceof WriteOperation) {
            return ptExecutor.execute((WriteOperation)operation, (ReadConcern)args[1], null);
        } else {
            return ptExecutor.execute((ReadOperation)operation, (ReadPreference)args[1], (ReadConcern)args[2], null);
        }
    }

    private void doShadowTable(Object operator, OperationAccessor operationAccessor,
        ShadowDatabaseConfig shadowDatabaseConfig) throws Exception {
        if (operationAccessor.isRead()) {
            setReadPtMongoNamespace(operator, operationAccessor, shadowDatabaseConfig);
        } else {
            setWritePtMongoNamespace(operator, operationAccessor, shadowDatabaseConfig);
        }
    }

    private ShadowDatabaseConfig getShadowDatabaseConfig(MongoClientDelegate mongoClientDelegate) {
        ClusterSettings clusterSettings = mongoClientDelegate.getCluster().getSettings();
        List<ServerAddress> serverAddresses = clusterSettings.getHosts();
        ShadowDatabaseConfig shadowDatabaseConfig = null;
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            for (ServerAddress serverAddress : serverAddresses) {
                if (config.getUrl().contains(serverAddress.toString())) {
                    shadowDatabaseConfig = config;
                    break;
                }
            }
        }
        return shadowDatabaseConfig;
    }

    private void setWritePtMongoNamespace(Object operation, OperationAccessor operationAccessor,
        ShadowDatabaseConfig shadowDatabaseConfig) throws Exception {
        setPtMongoNamespace(operation, operationAccessor, shadowDatabaseConfig, true);
    }

    private void setReadPtMongoNamespace(Object operation, OperationAccessor operationAccessor,
        ShadowDatabaseConfig shadowDatabaseConfig) throws Exception {
        setPtMongoNamespace(operation, operationAccessor, shadowDatabaseConfig, false);
    }

    private void setPtMongoNamespace(Object operation, OperationAccessor operationAccessor,
                                          ShadowDatabaseConfig shadowDatabaseConfig,
                                     boolean isWrite) throws Exception {
        //写操作未配置则直接抛异常
        MongoNamespace busMongoNamespace = operationAccessor.getMongoNamespace(operation);
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            if (isWrite) {
                //写操作未配置则直接抛异常
                ErrorReporter.Error error = ErrorReporter.buildError()
                        .setErrorType(ErrorTypeEnum.DataSource)
                        .setErrorCode("datasource-0005")
                        .setMessage("mongo 未配置对应影子表1:" + busMongoNamespace.getFullName())
                        .setDetail("mongo 未配置对应影子表1:" + busMongoNamespace.getFullName());
                error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
                error.report();
                throw new PressureMeasureError("mongo 未配置对应影子表1:" + busMongoNamespace.getFullName());
            }else{
                //读操作不包含则直接读取业务表
                return;
            }
        }
        MongoNamespace ptMongoNamespace = new MongoNamespace(busMongoNamespace.getDatabaseName(), shadowTableName);
        operationAccessor.setMongoNamespace(operation, ptMongoNamespace);
    }

    /**
     * 获取影子表时需要忽略配置与实际的大小写差异
     *
     * @param shadowDatabaseConfig 影子配置
     * @param bizTableName         业务表名
     * @return
     */
    private String getShadowTableName(ShadowDatabaseConfig shadowDatabaseConfig, String bizTableName) {
        if (shadowDatabaseConfig == null) {
            return null;
        }
        //如果已经被处理未pt前缀表，则直接返回
        if (Pradar.isClusterTestPrefix(bizTableName)){
            return bizTableName;
        }
        for (Map.Entry<String, String> entry : shadowDatabaseConfig.getBusinessShadowTables().entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), bizTableName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    protected void clean() {
        Caches.clean();
    }
}
