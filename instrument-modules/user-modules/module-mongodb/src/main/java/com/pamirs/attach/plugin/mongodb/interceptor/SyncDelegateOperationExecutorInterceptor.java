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
package com.pamirs.attach.plugin.mongodb.interceptor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.connection.ClusterSettings;
import com.pamirs.attach.plugin.dynamic.reflect.ReflectionUtils;
import com.pamirs.pradar.ConfigNames;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @author angju
 * @date 2021/3/31 21:19
 */
public class SyncDelegateOperationExecutorInterceptor extends ParametersWrapperInterceptorAdaptor {

    private Map<Object, Field> objectFieldMap = new ConcurrentHashMap<Object, Field>(32, 1);

    private Map<String, Integer> operationNumMap = new HashMap<String, Integer>(32, 1);

    private MongoClientDelegate mongoClientDelegate = null;

    public SyncDelegateOperationExecutorInterceptor() {
        operationNumMap.put("FindOperation", 1);
        operationNumMap.put("CountOperation", 1);
        operationNumMap.put("DistinctOperation", 1);
        operationNumMap.put("GroupOperation", 1);
        operationNumMap.put("ListIndexesOperation", 1);
        operationNumMap.put("MapReduceWithInlineResultsOperation", 1);
        operationNumMap.put("ParallelCollectionScanOperation", 1);
        operationNumMap.put("AggregateOperation", 1);

        //写操作
        operationNumMap.put("MixedBulkWriteOperation", 2);
        operationNumMap.put("BaseFindAndModifyOperation", 2);
        operationNumMap.put("BaseWriteOperation", 2);
        operationNumMap.put("FindAndDeleteOperation", 2);
        operationNumMap.put("FindAndReplaceOperation", 2);
        operationNumMap.put("FindAndUpdateOperation", 2);
        operationNumMap.put("MapReduceToCollectionOperation", 2);
        operationNumMap.put("InsertOperation", 2);
    }

    @Override
    public void clean() {
        objectFieldMap.clear();
        operationNumMap.clear();
    }

    @Override
    public Object[] getParameter0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return advice.getParameterArray();
        }
        Object[] args = advice.getParameterArray();

        Integer operationNum = operationNumMap.get(args[0].getClass().getSimpleName());
        if (operationNum == null) {
            LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
            throw new PressureMeasureError("[4]mongo not support pressure operation class is " + args[0].getClass().getName());
        }


        if (mongoClientDelegate == null) {
            Field field = null;
            try {
                field = advice.getTarget().getClass().getDeclaredField("this$0");
                field.setAccessible(true);
                mongoClientDelegate = (MongoClientDelegate) field.get(advice.getTarget());
            } catch (Throwable e) {
                LOGGER.error("DelegateOperationExecutorInterceptor error {}", e);
            } finally {
                if (field != null) {
                    field.setAccessible(false);
                }
            }

        }
        ClusterSettings clusterSettings = ReflectionUtils.get(ReflectionUtils.get(mongoClientDelegate, "cluster"), "settings");
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

        final Field field = objectFieldMap.get(args[0].getClass());
        if (field == null) {
            final Field namespace;
            if (isAggregateOperationInstance(args[0])) {
                namespace = ReflectionUtils.findField(ReflectionUtils.get(args[0], "wrapped").getClass(), "namespace");
            } else {
                namespace = ReflectionUtils.findField(args[0].getClass(),"namespace");
            }
            namespace.setAccessible(Boolean.TRUE);
            objectFieldMap.put(args[0].getClass(), namespace);
        }

        MongoNamespace busMongoNamespace;
        if (isAggregateOperationInstance(args[0])) {
            busMongoNamespace = ReflectionUtils.invoke(args[0], "getNamespace");
        } else {
            busMongoNamespace = (MongoNamespace) objectFieldMap.get(args[0].getClass()).get(args[0]);
        }
        switch (operationNum) {
            case 1:
                setReadPtMongoNamespace(args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 2:
                setWritePtMongoNamespace(args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            default:
                LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
                throw new PressureMeasureError("[5]mongo not support pressure operation class is " + args[0].getClass().getName());
        }

        return advice.getParameterArray();
    }

    private boolean isAggregateOperationInstance(Object obj) {
        return obj.getClass().getSimpleName().equals("AggregateOperation");
    }


    private void setWritePtMongoNamespace(Object target, MongoNamespace busMongoNamespace, ShadowDatabaseConfig shadowDatabaseConfig) throws IllegalAccessException, NoSuchFieldException {
        //写操作未配置则直接抛异常
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongo 未配置对应影子表5:" + busMongoNamespace.getFullName())
                    .setDetail("mongo 未配置对应影子表5:" + busMongoNamespace.getFullName());
            error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
            error.report();
            throw new PressureMeasureError("mongo 未配置对应影子表5:" + busMongoNamespace.getFullName());
        }
        setPtMongoNamespace(target, busMongoNamespace, shadowTableName);
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
        for (Map.Entry<String, String> entry : shadowDatabaseConfig.getBusinessShadowTables().entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), bizTableName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void setReadPtMongoNamespace(Object target, MongoNamespace busMongoNamespace, ShadowDatabaseConfig shadowDatabaseConfig) throws IllegalAccessException, NoSuchFieldException {
        //读操作不包含则直接读取业务表
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            return;
        }
        setPtMongoNamespace(target, busMongoNamespace, shadowTableName);
    }

    private void setPtMongoNamespace(Object target, MongoNamespace busMongoNamespace, String shadowTableName) throws NoSuchFieldException, IllegalAccessException {
        MongoNamespace ptMongoNamespace = new MongoNamespace(busMongoNamespace.getDatabaseName(), shadowTableName);
        final Class<?> operationClass = target.getClass();
        if (!objectFieldMap.containsKey(operationClass)) {
            Field nameSpaceField;
            try {
                nameSpaceField = operationClass.getDeclaredField("namespace");
            } catch (NoSuchFieldException e) {
                nameSpaceField = operationClass.getSuperclass().getDeclaredField("namespace");
            }

            nameSpaceField.setAccessible(true);
            objectFieldMap.put(operationClass, nameSpaceField);
        }
        if (isAggregateOperationInstance(target)) {
            target = ReflectionUtils.get(target, "wrapped");
        }
        objectFieldMap.get(operationClass).set(target, ptMongoNamespace);
    }
}
