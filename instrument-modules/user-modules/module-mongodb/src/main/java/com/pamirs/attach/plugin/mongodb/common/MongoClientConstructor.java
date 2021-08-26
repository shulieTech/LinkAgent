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
package com.pamirs.attach.plugin.mongodb.common;

import com.mongodb.*;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;

import java.util.List;
import java.util.Map;

/**
 * @author angju
 * @date 2020/8/18 10:44
 */

public class MongoClientConstructor {
    public static void doBefore(ThreadLocal<Boolean> SWITCHER, MongoClient mongoClient, MongoClientOptions mongoClientOptions) {
        if (SWITCHER.get()) {
            SWITCHER.set(false);
            return;
        }

        if (GlobalConfig.getInstance().isEmptyShadowDatabaseConfigs()) {
            return;
        }
        //获取配置，构造insertOperation需要使用
        MongoOperationUtil.retryWrites = (mongoClient).getMongoClientOptions().getRetryWrites();

        List<ServerAddress> addressList = (mongoClient).getAllAddress();
        for (Map.Entry<String, ShadowDatabaseConfig> entry : GlobalConfig.getInstance().getShadowDatasourceConfigs().entrySet()) {
            String ptUrl = entry.getValue().getShadowUrl();
            String businessUrl = entry.getValue().getUrl();
            for (ServerAddress address : addressList) {
                if (businessUrl != null && businessUrl.contains(address.toString())) {
                    if (!MongoClientHolder.mongoClientMap.containsKey(address.toString())) {
                        MongoClientHolder.mongoHolder.set((Mongo) mongoClient);
                        MongoClientHolder.mongoClientMap.put(address.toString(), constructorMongoClient(ptUrl, mongoClientOptions, SWITCHER));
                        break;
                    } else {
                        throw new PressureMeasureError("配置了相同地址的的影子库!");
                    }

                }
            }
        }
    }

    private static MongoClient constructorMongoClient(String url, MongoClientOptions mongoClientOptions, ThreadLocal<Boolean> SWITCHER) {
        SWITCHER.set(true);
        return new MongoClient(new MongoClientURI(url, MongoClientOptions.builder(mongoClientOptions)
                .minConnectionsPerHost(1).connectionsPerHost(1)));
    }
}
