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
import com.mongodb.MongoClientOptions;
import com.pamirs.attach.plugin.mongodb.common.MongoClientConstructor;
import com.pamirs.attach.plugin.mongodb.common.MongoClientPtCreate;
import com.pamirs.attach.plugin.mongodb.destroy.MogoDestroyed;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;

/**
 * @author angju
 * @date 2020/8/18 10:41
 */
@Destroyable(MogoDestroyed.class)
public class MongoDBMongoConstructor_3_Interceptor extends AroundInterceptor {

    @Override
    public void doBefore(Advice advice) {
        Object[] args = advice.getParameterArray();
        Object target = advice.getTarget();
        MongoClientConstructor.doBefore(MongoClientPtCreate.createPtMongoClient, (MongoClient) target, (MongoClientOptions) args[1]);
    }
}
