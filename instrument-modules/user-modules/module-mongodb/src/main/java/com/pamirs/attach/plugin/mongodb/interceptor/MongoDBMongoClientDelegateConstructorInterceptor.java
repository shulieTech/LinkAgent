package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.Mongo;
import com.mongodb.ServerAddress;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.internal.connection.MultiServerCluster;
import com.mongodb.internal.connection.SingleServerCluster;
import com.pamirs.attach.plugin.mongodb.common.MongoClientHolder;
import com.pamirs.attach.plugin.mongodb.common.MongoClientPtCreate;
import com.pamirs.attach.plugin.mongodb.obj.BusinessDelegateOperationExecutor;
import com.pamirs.attach.plugin.mongodb.obj.DelegateOperationExecutorWrapper;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.List;
import java.util.Map;

/**
 * @author angju
 * @date 2020/8/14 19:12
 * 3.8.2版本
 */
public class MongoDBMongoClientDelegateConstructorInterceptor extends ParametersWrapperInterceptorAdaptor {

    @Override
    public Object[] getParameter0(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();

        if (MongoClientPtCreate.createPtMongoClient.get()) {
            BusinessDelegateOperationExecutor businessDelegateOperationExecutor =
                    new BusinessDelegateOperationExecutor((MongoClientDelegate) target,
                            args[2]);
            args[3] = businessDelegateOperationExecutor;
            if (MongoClientHolder.mongoHolder.get() == null) {
                throw new PressureMeasureError("mongo 业务数据源找不到");
            }
            //存放业务数据源对象和影子对象的关系
            MongoClientHolder.mongoOperationExecutorMap.put(MongoClientHolder.mongoHolder.get(), businessDelegateOperationExecutor);
            MongoClientHolder.mongoHolder.remove();
            return args;
        }
        check(args[0], (Mongo) args[2]);
        BusinessDelegateOperationExecutor ptDelegateOperationExecutor = new BusinessDelegateOperationExecutor((MongoClientDelegate) target,
                args[2]);
        DelegateOperationExecutorWrapper delegateOperationExecutorWrapper = new DelegateOperationExecutorWrapper((MongoClientDelegate) target,
                args[2], ptDelegateOperationExecutor);
        args[3] = delegateOperationExecutorWrapper;
        return args;
    }


    /**
     * 判断是否影子库
     */
    private void check(Object baseCluster, Mongo businessMongo) {
        List<ServerAddress> addressList;
        if (baseCluster instanceof SingleServerCluster) {
            addressList = ((SingleServerCluster) baseCluster).getSettings().getHosts();
        } else {
            addressList = ((MultiServerCluster) baseCluster).getSettings().getHosts();
        }
        for (Map.Entry<String, ShadowDatabaseConfig> entry : GlobalConfig.getInstance().getShadowDatasourceConfigs().entrySet()) {
            String businessUrl = entry.getValue().getUrl();
            for (ServerAddress address : addressList) {
                if (businessUrl != null && businessUrl.contains(address.toString())) {
                    if (!MongoClientHolder.mongoClientMap.containsKey(address.toString())) {
                        MongoClientHolder.mongoIntegerMap.put(businessMongo, 2);
                        break;
                    } else {
                        throw new PressureMeasureError("配置了相同地址的的影子库!");
                    }

                }
            }
        }

        for (ServerAddress address : addressList) {
            ShadowDatabaseConfig shadowDatabaseConfig = GlobalConfig.getInstance().getShadowDatabaseConfig(address.toString());
            if (shadowDatabaseConfig == null) {
                continue;
            }
            if (!shadowDatabaseConfig.isShadowTable()) {
                continue;
            }

            Map<String, String> map = shadowDatabaseConfig.getBusinessShadowTables();
            if (null != map && map.size() > 0) {
                MongoClientHolder.mongoIntegerMap.put(businessMongo, 1);
                MongoClientHolder.mongoTableMappingMap.put(businessMongo, map);
                break;
            }
        }
    }
}
