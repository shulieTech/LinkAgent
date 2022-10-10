/*
 * Copyright 2021 Shulie Technology, Co.Ltd
 * Email: shulie@shulie.io
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pamirs.attach.plugin.mongodb.interceptor;

import com.mongodb.MongoNamespace;
import com.mongodb.ServerAddress;
import com.mongodb.connection.Cluster;
import com.mongodb.operation.AsyncReadOperation;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.ParametersWrapperInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.druid.support.json.JSONUtils;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author ocean_wll
 * @Date 2022/9/29 15:19
 */
public class AsyncOperationExecutorImplInterceptor extends ParametersWrapperInterceptorAdaptor {
    protected final Logger logger = LoggerFactory.getLogger(AsyncOperationExecutorImplInterceptor.class);

    private volatile Field namespaceField;
    private volatile Field collectionNameField;
    private volatile Field fullNameField;
    private Field mongoClientField;
    private Method getClusterMethod;

    @Override
    protected Object[] getParameter0(Advice advice) throws Throwable {
        Object[] args = advice.getParameterArray();
        if (!Pradar.isClusterTest()) {
            return args;
        }
        List<ServerAddress> serverAddresses = getHosts(advice.getTarget());
        Map<String, String> businessShadowTables = null;
        for (ShadowDatabaseConfig shadowDatabaseConfig : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            for (ServerAddress s : serverAddresses) {
                if (shadowDatabaseConfig.getDsType() == 1 && shadowDatabaseConfig.getUrl().contains(s.toString())) {
                    if (!shadowDatabaseConfig.getBusinessShadowTables().isEmpty()) {
                        businessShadowTables = shadowDatabaseConfig.getBusinessShadowTables();
                    }
                    break;
                }
            }
        }
        Object operation = args[0];
        if (businessShadowTables == null) {
            //没有影子表的配置，但是是读取的，默认就读取业务的
            if (operation instanceof AsyncReadOperation) {
                return args;
            } else {
                throw new PressureMeasureError("未配置影子表，业务地址为：" + JSONUtils.toJSONString(transHosts(serverAddresses)));
            }
        }

        setNamespaceField(operation.getClass());
        MongoNamespace mongoNamespace = getMongoNamespace(operation);

        //是否配置了读的影子表
        String collectionName = mongoNamespace.getCollectionName();
        //没有配置影子表，读取业务
        if (operation instanceof AsyncReadOperation && !businessShadowTables.containsKey(collectionName)) {
            return args;
        }
        if (!(operation instanceof AsyncReadOperation) && !businessShadowTables.containsKey(collectionName)) {
            throw new PressureMeasureError("写操作未配置影子表，业务地址为：" + JSONUtils.toJSONString(serverAddresses));
        }

        setCollectionNameField(mongoNamespace.getClass());
        setFullNameField(mongoNamespace.getClass());
        replaceFullName(mongoNamespace);
        replaceCollectionName(mongoNamespace);
        return args;
    }


    private List<String> transHosts(List<ServerAddress> serverAddresses) {
        List<String> list = new ArrayList();
        for (ServerAddress serverAddress : serverAddresses) {
            list.add(serverAddress.toString());
        }
        return list;
    }

    private List<ServerAddress> getHosts(Object operationExecutorImpl) {
        mongoClientField = getField(operationExecutorImpl.getClass(), "mongoClient");
        if (mongoClientField == null) {
            throw new PressureMeasureError("无法操作影子表替换,mongoClientField is null");
        }
        Object mongoClientImpl = null;
        try {
            mongoClientImpl = mongoClientField.get(operationExecutorImpl);
            getClusterMethod = mongoClientImpl.getClass().getDeclaredMethod("getCluster");
            getClusterMethod.setAccessible(true);
            Cluster cluster = (Cluster) getClusterMethod.invoke(mongoClientImpl, null);
            return cluster.getSettings().getHosts();
        } catch (IllegalAccessException illegalAccessException) {
            throw new PressureMeasureError("获取mongoClientImpl失败");
        } catch (NoSuchMethodException e) {
            throw new PressureMeasureError("获取getClusterMethod失败");
        } catch (InvocationTargetException e) {
            throw new PressureMeasureError("获取cluster失败");
        }
    }


    private void replaceFullName(MongoNamespace mongoNamespace) {
        try {
            String databaseName = mongoNamespace.getDatabaseName();
            String collectionName = mongoNamespace.getCollectionName();
            fullNameField.set(mongoNamespace, databaseName + ".PT_" + collectionName);
        } catch (IllegalAccessException illegalAccessException) {
            throw new PressureMeasureError("替换影子表失败");
        }
    }

    private void replaceCollectionName(MongoNamespace mongoNamespace) {
        String collectionName = mongoNamespace.getCollectionName();
        try {
            collectionNameField.set(mongoNamespace, "PT_" + collectionName);
        } catch (IllegalAccessException illegalAccessException) {
            throw new PressureMeasureError("替换影子表失败");
        }
    }


    private MongoNamespace getMongoNamespace(Object operation) {
        MongoNamespace mongoNamespace = null;
        try {
            mongoNamespace = (MongoNamespace) namespaceField.get(operation);
        } catch (IllegalAccessException illegalAccessException) {
            //ignore
            logger.error("get namespace is failed");
        }
        if (mongoNamespace == null) {
            throw new PressureMeasureError("无法操作影子表替换,mongoNamespace is null");
        }
        return mongoNamespace;
    }

    private void setFullNameField(Class c) {
        if (fullNameField == null) {
            synchronized (this) {
                if (fullNameField == null) {
                    fullNameField = getField(c, "fullName");
                }
            }
        }
        if (fullNameField == null) {
            throw new PressureMeasureError("无法操作影子表替换,fullNameField is null");
        }
    }

    private void setCollectionNameField(Class c) {
        if (collectionNameField == null) {
            synchronized (this) {
                if (collectionNameField == null) {
                    collectionNameField = getField(c, "collectionName");
                }
            }
        }
        if (collectionNameField == null) {
            throw new PressureMeasureError("无法操作影子表替换,collectionNameField is null");
        }
    }

    private void setNamespaceField(Class c) {
        if (namespaceField == null) {
            synchronized (this) {
                if (namespaceField == null) {
                    namespaceField = getField(c, "namespace");
                }
            }
        }
        if (namespaceField == null) {
            throw new PressureMeasureError("无法操作影子表替换,无namespace字段，class is " + c.getName());
        }
    }

    private Field getField(Class c, String fieldName) {
        try {
            Field field = c.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException noSuchFieldException) {
            logger.error("无法获取field，" + fieldName + "class is {}" + c.getName());
            return null;
        }
    }


}
