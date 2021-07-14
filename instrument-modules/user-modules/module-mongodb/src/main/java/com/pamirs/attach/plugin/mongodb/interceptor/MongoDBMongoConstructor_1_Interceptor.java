package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.*;
import com.pamirs.attach.plugin.mongodb.common.MongoClientHolder;
import com.pamirs.attach.plugin.mongodb.common.MongoClientPtCreate;
import com.pamirs.attach.plugin.mongodb.common.MongoOperationUtil;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

import java.util.List;
import java.util.Map;

/**
 * @author angju
 * @date 2020/8/10 23:28
 */
public class MongoDBMongoConstructor_1_Interceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();

        if (MongoClientPtCreate.createPtMongoClient.get()) {
            MongoClientPtCreate.createPtMongoClient.set(false);
            return;
        }


        if (GlobalConfig.getInstance().isEmptyShadowDatabaseConfigs()) {
            return;
        }
        //获取配置，构造insertOperation需要使用
        MongoOperationUtil.retryWrites = ((MongoClient) target).getMongoClientOptions().getRetryWrites();

        List<ServerAddress> addressList = ((MongoClient) target).getAllAddress();

        for (Map.Entry<String, ShadowDatabaseConfig> entry : GlobalConfig.getInstance().getShadowDatasourceConfigs().entrySet()) {
            String ptUrl = entry.getValue().getShadowUrl();
            String businessUrl = entry.getValue().getUrl();
            for (ServerAddress address : addressList) {
                if (businessUrl != null && businessUrl.contains(address.toString())) {
                    if (!MongoClientHolder.mongoClientMap.containsKey(address.toString())) {
                        MongoClientHolder.mongoHolder.set((Mongo) target);
                        MongoClientHolder.mongoClientMap.put(address.toString(), constructorMongoClient(ptUrl, ((MongoClientURI) (args[0])).getOptions()));
                        break;
                    } else {
                        throw new PressureMeasureError("配置了相同地址的的影子库!");
                    }

                }
            }
        }


    }


    private MongoClient constructorMongoClient(String url, MongoClientOptions mongoClientOptions) {
        MongoClientPtCreate.createPtMongoClient.set(true);
        return new MongoClient(new MongoClientURI(url, MongoClientOptions.builder(mongoClientOptions)));
    }

}
