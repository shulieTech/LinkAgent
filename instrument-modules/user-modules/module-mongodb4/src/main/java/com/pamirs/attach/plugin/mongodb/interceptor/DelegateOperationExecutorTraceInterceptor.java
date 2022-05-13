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

import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.internal.connection.Cluster;
import com.pamirs.attach.plugin.mongodb.utils.Caches;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessor;
import com.pamirs.attach.plugin.mongodb.utils.OperationAccessorFactory;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.interceptor.SpanRecord;
import com.pamirs.pradar.interceptor.TraceInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import com.shulie.instrument.simulator.api.util.ReflectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author angju
 * @date 2021/3/31 21:19
 */
public class DelegateOperationExecutorTraceInterceptor extends TraceInterceptorAdaptor {

    @Override
    public String getPluginName() {
        return "mongodb";
    }

    @Override
    public int getPluginType() {
        return 4;
    }

    @Override
    public SpanRecord beforeTrace(Advice advice) {
        Object[] args = advice.getParameterArray();
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setMiddlewareName("mongodb");
        Class operationClass = args[0].getClass();
        spanRecord.setMethod(operationClass.getSimpleName());
        spanRecord.setService("unknown");
        try {
            Object mongoClientDelegate = Caches.getMongoClientDelegate(advice.getTarget());
            ShadowDatabaseConfig databaseConfig = null;
            if (Pradar.isClusterTest()) {
                databaseConfig = getShadowDatabaseConfig(mongoClientDelegate);
                if (databaseConfig == null) {
                    return spanRecord;
                }
                String shadowUrl = databaseConfig.getShadowUrl();
                spanRecord.setRemoteIp(shadowUrl);
            } else {
                String addressesStr = StringUtils.join(((Cluster) (ReflectionUtils.getFieldValue(mongoClientDelegate, "cluster"))).getSettings().getHosts(), ",");
                spanRecord.setRemoteIp(addressesStr);
            }

            OperationAccessor operationAccessor = OperationAccessorFactory.getOperationAccessor(operationClass);
            if (operationAccessor != null) {
                try {
                    String service;
                    MongoNamespace namespace = operationAccessor.getMongoNamespace(args[0]);
                    String collectionName = namespace.getCollectionName();

                    if (Pradar.isClusterTest()) {
                        if (databaseConfig.isShadowTable()) {
                            service = namespace.getDatabaseName() + "." + Pradar.addClusterTestPrefix(collectionName);
                        } else if (databaseConfig.isShadowDatabase()) {
                            String shadowSchema = databaseConfig.getShadowSchema();
                            service = shadowSchema.substring(0, shadowSchema.indexOf("?")) + "." + collectionName;
                        } else {
                            String shadowSchema = databaseConfig.getShadowSchema();
                            service = shadowSchema.substring(0, shadowSchema.indexOf("?")) + "." + Pradar.addClusterTestPrefix(collectionName);
                        }
                        spanRecord.setService(service);
                    } else {
                        service = namespace.getFullName();
                    }

                    spanRecord.setService(service);
                } catch (Exception e) {
                    LOGGER.error("not support operation class is {} ", args[0].getClass().getName());
                }
            }
        } catch (Exception e) {
            LOGGER.error("can not get mongoClientDelegate");
        }
        return spanRecord;
    }

    @Override
    public SpanRecord afterTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResultCode("200");
        return spanRecord;
    }

    @Override
    public SpanRecord exceptionTrace(Advice advice) {
        SpanRecord spanRecord = new SpanRecord();
        spanRecord.setResponse(advice.getThrowable());
        spanRecord.setResultCode("500");
        return spanRecord;
    }

    private ShadowDatabaseConfig getShadowDatabaseConfig(Object mongoClientDelegate) {
        ClusterSettings clusterSettings = ((Cluster) (ReflectionUtils.getFieldValue(mongoClientDelegate, "cluster"))).getSettings();
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
}
