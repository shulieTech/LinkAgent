package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.connection.ClusterSettings;
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
 * @date 2021/3/31 21:19
 */
public class DelegateOperationExecutorInterceptor extends ParametersWrapperInterceptorAdaptor {

    private Map<Object, Field> objectFieldMap = new ConcurrentHashMap<Object, Field>(32, 1);

    private Map<Class, Integer> operationNumMap = new HashMap<Class, Integer>(32, 1);

    private MongoClientDelegate mongoClientDelegate = null;

    public DelegateOperationExecutorInterceptor() {
        operationNumMap.put(FindOperation.class, 1);
        operationNumMap.put(CountOperation.class, 2);
        operationNumMap.put(DistinctOperation.class, 3);
        operationNumMap.put(GroupOperation.class, 4);
        operationNumMap.put(ListIndexesOperation.class, 5);
        operationNumMap.put(MapReduceWithInlineResultsOperation.class, 6);
        operationNumMap.put(ParallelCollectionScanOperation.class, 7);

        //写操作
        operationNumMap.put(MixedBulkWriteOperation.class, 8);
        operationNumMap.put(BaseFindAndModifyOperation.class, 9);
        operationNumMap.put(BaseWriteOperation.class, 10);
        operationNumMap.put(FindAndDeleteOperation.class, 11);
        operationNumMap.put(FindAndReplaceOperation.class, 12);
        operationNumMap.put(FindAndUpdateOperation.class, 13);
        operationNumMap.put(MapReduceToCollectionOperation.class, 14);
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
            throw new PressureMeasureError("mongo not support pressure operation class is " + args[0].getClass().getName());
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

        ClusterSettings clusterSettings = mongoClientDelegate.getCluster().getSettings();
        List<ServerAddress> serverAddresses = clusterSettings.getHosts();
        ShadowDatabaseConfig shadowDatabaseConfig = null;
        for (ServerAddress serverAddress : serverAddresses) {
            shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(serverAddress.toString());
            if (shadowDatabaseConfig != null) {
                break;
            }
        }

        MongoNamespace busMongoNamespace;
        switch (operationNum) {
            case 1:
                busMongoNamespace = ((FindOperation) args[0]).getNamespace();
                setReadPtMongoNamespace(FindOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 2:
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(CountOperation.class).get(args[0]);
                setReadPtMongoNamespace(CountOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 3:
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(DistinctOperation.class).get(args[0]);
                setReadPtMongoNamespace(DistinctOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 4:
                busMongoNamespace = ((GroupOperation) args[0]).getNamespace();
                setReadPtMongoNamespace(GroupOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 5:
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(ListIndexesOperation.class).get(args[0]);
                setReadPtMongoNamespace(ListIndexesOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 6:
                busMongoNamespace = ((MapReduceWithInlineResultsOperation) args[0]).getNamespace();
                setReadPtMongoNamespace(MapReduceWithInlineResultsOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 7:
                busMongoNamespace = (MongoNamespace) objectFieldMap.get(ParallelCollectionScanOperation.class).get(args[0]);
                setReadPtMongoNamespace(ParallelCollectionScanOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 8:
                busMongoNamespace = ((MixedBulkWriteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(MixedBulkWriteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 9:
                busMongoNamespace = ((BaseFindAndModifyOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(BaseFindAndModifyOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 10:
                busMongoNamespace = ((BaseWriteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(BaseWriteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 11:
                busMongoNamespace = ((FindAndDeleteOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(FindAndDeleteOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 12:
                busMongoNamespace = ((FindAndReplaceOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(FindAndReplaceOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 13:
                busMongoNamespace = ((FindAndUpdateOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(FindAndUpdateOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            case 14:
                busMongoNamespace = ((MapReduceToCollectionOperation) (args[0])).getNamespace();
                setWritePtMongoNamespace(MapReduceToCollectionOperation.class, args[0], busMongoNamespace, shadowDatabaseConfig);
                break;
            default:
                LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
                throw new PressureMeasureError("mongo not support pressure operation class is " + args[0].getClass().getName());
        }

        return advice.getParameterArray();
    }


    private void setWritePtMongoNamespace(Class operationClass, Object target, MongoNamespace busMongoNamespace, ShadowDatabaseConfig shadowDatabaseConfig) throws IllegalAccessException, NoSuchFieldException {
        //写操作未配置则直接抛异常
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            ErrorReporter.Error error = ErrorReporter.buildError()
                    .setErrorType(ErrorTypeEnum.DataSource)
                    .setErrorCode("datasource-0005")
                    .setMessage("mongo 未配置对应影子表:" + busMongoNamespace.getFullName())
                    .setDetail("mongo 未配置对应影子表:" + busMongoNamespace.getFullName());
            error.closePradar(ConfigNames.SHADOW_DATABASE_CONFIGS);
            error.report();
            throw new PressureMeasureError("mongo 未配置对应影子表:" + busMongoNamespace.getFullName());
        }
        setPtMongoNamespace(operationClass, target, busMongoNamespace, shadowTableName);
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

    private void setReadPtMongoNamespace(Class operationClass, Object target, MongoNamespace busMongoNamespace, ShadowDatabaseConfig shadowDatabaseConfig) throws IllegalAccessException, NoSuchFieldException {
        //读操作不包含则直接读取业务表
        String shadowTableName = getShadowTableName(shadowDatabaseConfig, busMongoNamespace.getCollectionName());
        if (shadowTableName == null) {
            return;
        }
        setPtMongoNamespace(operationClass, target, busMongoNamespace, shadowTableName);
    }

    private void setPtMongoNamespace(Class operationClass, Object target, MongoNamespace busMongoNamespace, String shadowTableName) throws NoSuchFieldException, IllegalAccessException {
        MongoNamespace ptMongoNamespace = new MongoNamespace(busMongoNamespace.getDatabaseName(), shadowTableName);
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
        objectFieldMap.get(operationClass).set(target, ptMongoNamespace);
    }
}
