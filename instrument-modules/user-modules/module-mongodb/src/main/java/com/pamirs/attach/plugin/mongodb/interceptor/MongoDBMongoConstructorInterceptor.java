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

import com.mongodb.*;
import com.pamirs.attach.plugin.mongodb.common.MongoClientHolder;
import com.pamirs.attach.plugin.mongodb.common.MongoClientPtCreate;
import com.pamirs.attach.plugin.mongodb.destroy.MogoDestroyed;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.AroundInterceptor;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.shulie.instrument.simulator.api.annotation.Destroyable;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * @author angju
 * @date 2020/8/6 23:23
 */
@Destroyable(MogoDestroyed.class)
public class MongoDBMongoConstructorInterceptor extends AroundInterceptor {

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

        String currentAddress = ((MongoClient) target).getAddress().toString();

        for (Map.Entry<String, ShadowDatabaseConfig> entry : GlobalConfig.getInstance().getShadowDatasourceConfigs().entrySet()) {
            String url = entry.getValue().getShadowUrl();
            if (url != null && url.contains(currentAddress)) {
                if (!MongoClientHolder.mongoClientMap.containsKey(currentAddress)) {
                    MongoClientHolder.mongoHolder.set((Mongo) target);
                    MongoClientHolder.mongoClientMap.put(currentAddress, constructorMongoClient(url, (MongoClientOptions) args[2], (List<MongoCredential>) args[1]));
                } else {
                    throw new PressureMeasureError("配置了相同地址的的影子库!");
                }

            }

        }


    }


    private MongoClient constructorMongoClient(String url, MongoClientOptions options, List<MongoCredential> credentialsList) {

        Map<String, String> connectionMap = resolveUrl(url);

        List<MongoCredential> credentials = new ArrayList<MongoCredential>(credentialsList.size());

        credentials.add(MongoCredential.createCredential(connectionMap.get("username"), connectionMap.get("database"),
                connectionMap.get("password").toCharArray()));
        String host = connectionMap.get("host");
        Integer port = Integer.valueOf(connectionMap.get("port"));
        MongoClientPtCreate.createPtMongoClient.set(true);
        return new MongoClient(
                Collections.singletonList(new ServerAddress(host, port)), credentials,
                options);
    }

    /**
     * mongodb://daniel:123456@localhost:27017/test
     *
     * @param url
     * @return
     */
    public Map<String, String> resolveUrl(String url) {
        Map<String, String> map = new HashMap<String, String>();
        String s[] = StringUtils.split(url.split("//")[1], '/');
        String database = s[1];
        String[] connectionInfo = StringUtils.split(s[0], '@');
        String[] accountInfo = StringUtils.split(connectionInfo[0], ':');
        String[] addressInfo = StringUtils.split(connectionInfo[1],':');
        String username = accountInfo[0];
        String password = accountInfo[1];
        String host = addressInfo[0];
        String port = addressInfo[1];
        map.put("database", database);
        map.put("username", username);
        map.put("password", password);
        map.put("host", host);
        map.put("port", port);
        return map;
    }
}
