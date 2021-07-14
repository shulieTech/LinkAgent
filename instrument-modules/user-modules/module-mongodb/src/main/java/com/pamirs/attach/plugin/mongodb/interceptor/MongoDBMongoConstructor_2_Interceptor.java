package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.pamirs.attach.plugin.mongodb.common.MongoClientConstructor;
import com.pamirs.attach.plugin.mongodb.common.MongoClientPtCreate;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author angju
 * @date 2020/8/17 20:21
 */
public class MongoDBMongoConstructor_2_Interceptor extends AroundInterceptor {


    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        MongoClientConstructor.doBefore(MongoClientPtCreate.createPtMongoClient, (MongoClient) target, (MongoClientOptions) args[2]);
    }
}
