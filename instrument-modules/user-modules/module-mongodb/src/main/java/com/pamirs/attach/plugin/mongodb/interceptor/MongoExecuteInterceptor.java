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

import com.mongodb.MongoClient;
import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.operation.*;
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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author angju
 * @date 2021/4/2 13:51
 */
public class MongoExecuteInterceptor extends ParametersWrapperInterceptorAdaptor {
    private Map<Object, Field> objectFieldMap = new ConcurrentHashMap<Object, Field>(32, 1);


    private Map<Class, Integer> operationNumMap = new HashMap<Class, Integer>(32, 1);
    private final static int FIND = 1;
    private final static int COUNT = 2;
    private final static int DISTINCT = 3;
    private final static int GROUP = 4;
    private final static int LIST_INDEXES = 5;
    private final static int MAP_REDUCE_WITH_INLINE = 6;
    private final static int PARALLEL_COLLECTION_SCAN = 7;
    private final static int MIXED_BULK_WRITE = 8;
    private final static int BASE_WRITE = 10;
    private final static int FIND_AND_DELETE = 11;
    private final static int FIND_AND_REPLACE = 12;
    private final static int FIND_AND_UPDATE = 13;
    private final static int MAP_REDUCE_TO_COLLECTION = 14;
    private final static int INSERT_TO_COLLECTION = 15;
    private final static int UPDATE_OPERATION = 16;
    private final static int DELETE_OPERATION = 17;



    public MongoExecuteInterceptor() {
        //读操作
        operationNumMap.put(FindOperation.class, FIND);
        operationNumMap.put(CountOperation.class, COUNT);
        operationNumMap.put(DistinctOperation.class, DISTINCT);
        operationNumMap.put(GroupOperation.class, GROUP);
        operationNumMap.put(ListIndexesOperation.class, LIST_INDEXES);
        operationNumMap.put(MapReduceWithInlineResultsOperation.class, MAP_REDUCE_WITH_INLINE);
        operationNumMap.put(ParallelCollectionScanOperation.class, PARALLEL_COLLECTION_SCAN);

        //写操作
        operationNumMap.put(MixedBulkWriteOperation.class, MIXED_BULK_WRITE);
//        operationNumMap.put(BaseFindAndModifyOperation.class, 9);
        operationNumMap.put(BaseWriteOperation.class, BASE_WRITE);
        operationNumMap.put(FindAndDeleteOperation.class, FIND_AND_DELETE);
        operationNumMap.put(FindAndReplaceOperation.class, FIND_AND_REPLACE);
        operationNumMap.put(FindAndUpdateOperation.class, FIND_AND_UPDATE);
        operationNumMap.put(MapReduceToCollectionOperation.class, MAP_REDUCE_TO_COLLECTION);
        operationNumMap.put(InsertOperation.class, INSERT_TO_COLLECTION);
        operationNumMap.put(com.mongodb.operation.UpdateOperation.class, UPDATE_OPERATION);
        operationNumMap.put(com.mongodb.operation.DeleteOperation.class, DELETE_OPERATION);


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

        Integer operationNum = operationNumMap.get(args[0].getClass());
        if (operationNum == null) {
            LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
            throw new PressureMeasureError("[2]mongo not support pressure operation class is " + args[0].getClass().getName());
        }

        List<ServerAddress> serverAddresses = ((MongoClient) advice.getTarget()).getAllAddress();
        ShadowDatabaseConfig shadowDatabaseConfig = getShadowDatabaseConfig(serverAddresses);

        if (operationNum > 7 && shadowDatabaseConfig == null){
            ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongo 未配置对应影子表或者影子库")
                    .setDetail("mongo 未配置对应影子表或者影子库");
            error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
            error.report();
            throw new PressureMeasureError("mongo 未配置对应影子表或者影子库");
        }

        if (shadowDatabaseConfig.isShadowDatabase()){
            return advice.getParameterArray();
        }


        MongoNamespace busMongoNamespace;
        switch (operationNum) {
            case FIND:
                objectFieldMapAdd(FindOperation.class);
                busMongoNamespace = ((FindOperation) args[0]).getNamespace();
                setReadPtMongoNamespace(FindOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;

            case COUNT:
                objectFieldMapAdd(CountOperation.class);
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(CountOperation.class).get(args[0]);
                setReadPtMongoNamespace(CountOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case DISTINCT:
                objectFieldMapAdd(DistinctOperation.class);
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(DistinctOperation.class).get(args[0]);
                setReadPtMongoNamespace(DistinctOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case GROUP:
                objectFieldMapAdd(GroupOperation.class);
                busMongoNamespace = ((GroupOperation) args[0]).getNamespace();
                setReadPtMongoNamespace(GroupOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case LIST_INDEXES:
                objectFieldMapAdd(ListIndexesOperation.class);
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(ListIndexesOperation.class).get(args[0]);
                setReadPtMongoNamespace(ListIndexesOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case MAP_REDUCE_WITH_INLINE:
                busMongoNamespace = ((MapReduceWithInlineResultsOperation) args[0]).getNamespace();
                setReadPtMongoNamespace(MapReduceWithInlineResultsOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case PARALLEL_COLLECTION_SCAN:
                objectFieldMapAdd(ParallelCollectionScanOperation.class);
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(ParallelCollectionScanOperation.class).get(args[0]);
                setReadPtMongoNamespace(ParallelCollectionScanOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case MIXED_BULK_WRITE:
                objectFieldMapAdd(MixedBulkWriteOperation.class);
                busMongoNamespace = ((MixedBulkWriteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(MixedBulkWriteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case BASE_WRITE:
                objectFieldMapAdd(BaseWriteOperation.class);
                busMongoNamespace = ((BaseWriteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(BaseWriteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case FIND_AND_DELETE:
                objectFieldMapAdd(FindAndDeleteOperation.class);
                busMongoNamespace = ((FindAndDeleteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(FindAndDeleteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case FIND_AND_REPLACE:
                objectFieldMapAdd(FindAndReplaceOperation.class);
                busMongoNamespace = ((FindAndReplaceOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(FindAndReplaceOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case FIND_AND_UPDATE:
                objectFieldMapAdd(FindAndUpdateOperation.class);
                busMongoNamespace = ((FindAndUpdateOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(FindAndUpdateOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case MAP_REDUCE_TO_COLLECTION:
                objectFieldMapAdd(MapReduceToCollectionOperation.class);
                busMongoNamespace = ((MapReduceToCollectionOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(MapReduceToCollectionOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case INSERT_TO_COLLECTION:
                objectFieldMapAdd(InsertOperation.class);
                busMongoNamespace = ((InsertOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(InsertOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case UPDATE_OPERATION:
                objectFieldMapAdd(UpdateOperation.class);
                busMongoNamespace = ((UpdateOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(UpdateOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case DELETE_OPERATION:
                objectFieldMapAdd(DeleteOperation.class);
                busMongoNamespace = ((DeleteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(DeleteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            default:
                LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
                throw new PressureMeasureError("[3]mongo not support pressure operation class is " + args[0].getClass().getName());
        }

        return advice.getParameterArray();
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

    private void setWritePtMongoNamespace(Class operationClass, Object target, MongoNamespace busMongoNamespace, ShadowDatabaseConfig shadowDatabaseConfig) throws IllegalAccessException, NoSuchFieldException {
        if (busMongoNamespace.getCollectionName().startsWith(Pradar.CLUSTER_TEST_PREFIX)){
            return;
        }
        //写操作未配置则直接抛异常
        if (shadowDatabaseConfig == null) {
            ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongo 未配置对应影子表2:" + busMongoNamespace.getFullName())
                    .setDetail("mongo 未配置对应影子表2:" + busMongoNamespace.getFullName());
            error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
            error.report();
            throw new PressureMeasureError("mongo 未配置对应影子表2:" + busMongoNamespace.getFullName());
        }
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongo 未配置对应影子表3:" + busMongoNamespace.getFullName())
                    .setDetail("mongo 未配置对应影子表3:" + busMongoNamespace.getFullName());
            error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
            error.report();
            throw new PressureMeasureError("mongo 未配置对应影子表3:" + busMongoNamespace.getFullName());
        }
        setPtMongoNamespace(operationClass, target, busMongoNamespace, shadowTableName);
    }

    private void setReadPtMongoNamespace(Class operationClass, Object target, MongoNamespace busMongoNamespace, ShadowDatabaseConfig shadowDatabaseConfig) throws IllegalAccessException, NoSuchFieldException {
        //读操作不包含则直接读取业务表
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            return;
        }
        setPtMongoNamespace(operationClass, target, busMongoNamespace, shadowTableName);
    }

    private void setPtMongoNamespace(Class operationClass, Object target, MongoNamespace busMongoNamespace, String ptCollectionsName) throws NoSuchFieldException, IllegalAccessException {
        MongoNamespace ptMongoNamespace = new MongoNamespace(busMongoNamespace.getDatabaseName(), ptCollectionsName);
        objectFieldMap.get(operationClass).set(target, ptMongoNamespace);
    }

    private void objectFieldMapAdd(Class operationClass) throws NoSuchFieldException {
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
    }

    private ShadowDatabaseConfig getShadowDatabaseConfig(List<ServerAddress> serverAddresses) {
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

}
