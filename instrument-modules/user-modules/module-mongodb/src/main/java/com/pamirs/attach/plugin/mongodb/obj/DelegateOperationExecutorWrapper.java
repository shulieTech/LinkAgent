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
package com.pamirs.attach.plugin.mongodb.obj;

import com.mongodb.Mongo;
import com.mongodb.MongoNamespace;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.client.internal.OperationExecutor;
import com.mongodb.lang.Nullable;
import com.mongodb.operation.FindOperation;
import com.mongodb.operation.ReadOperation;
import com.mongodb.operation.WriteOperation;
import com.pamirs.attach.plugin.mongodb.common.MongoClientHolder;
import com.pamirs.attach.plugin.mongodb.common.MongoOperationUtil;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;

import java.util.Map;

//import org.apache.commons.lang3.StringUtils;

/**
 * @author angju
 * @date 2020/8/14 18:41
 */
public class DelegateOperationExecutorWrapper implements OperationExecutor {

    private MongoClientDelegate mongoClientDelegate;

    private Object originator;

//    private OperationExecutor businessOperationExecutor;

    private OperationExecutor ptOperationExecutor;

    private DelegateOperationExecutorWrapper() {
    }

    public DelegateOperationExecutorWrapper(MongoClientDelegate mongoClientDelegate,
                                            Object originator,
//                                            OperationExecutor businessOperationExecutor,
                                            OperationExecutor ptOperationExecutor) {
        this.mongoClientDelegate = mongoClientDelegate;
        this.originator = originator;
        this.ptOperationExecutor = ptOperationExecutor;
//        this.businessOperationExecutor = businessOperationExecutor;
    }


    @Override
    public <T> T execute(final ReadOperation<T> operation, final ReadPreference readPreference, final ReadConcern readConcern) {
        if (!Pradar.isClusterTest()) {
            return ptOperationExecutor.execute(operation, readPreference, readConcern);
        }
        Integer flag = MongoClientHolder.mongoIntegerMap.get(originator);
        if (flag == 1) {//影子表
            String businessCollection = ((FindOperation) operation).getNamespace().getCollectionName();
            Map<String, String> tableMapping = MongoClientHolder.mongoTableMappingMap.get(originator);
            //没有配置，直接查业务
//            if (tableMapping == null || tableMapping.size() == 0 || StringUtils.isBlank(tableMapping.get(businessCollection))) {
//                return ptOperationExecutor.execute(operation, readPreference, readConcern);
//            }
            MongoNamespace ptNamespace = new MongoNamespace(((FindOperation) operation).getNamespace().getDatabaseName(), Pradar.addClusterTestPrefix(((FindOperation) operation).getNamespace().getCollectionName()));
            ReadOperation<T> ptOperation = MongoOperationUtil.generateFromFindOperation((FindOperation) operation, ptNamespace);
            return ptOperationExecutor.execute(ptOperation, readPreference, readConcern);
        } else if (flag == 2) {//影子库
            OperationExecutor operationExecutor = MongoClientHolder.mongoOperationExecutorMap.get(originator);
            MongoNamespace ptNamespace = new MongoNamespace(Pradar.addClusterTestPrefix(((FindOperation) operation).getNamespace().getDatabaseName()), ((FindOperation) operation).getNamespace().getCollectionName());
            ReadOperation<T> ptOperation = MongoOperationUtil.generateFromFindOperation((FindOperation) operation, ptNamespace);
            return operationExecutor.execute(ptOperation, readPreference, readConcern);
        } else { //什么都没配置 读取直接查业务
            return ptOperationExecutor.execute(operation, readPreference, readConcern);
        }
    }

    @Override
    public <T> T execute(final WriteOperation<T> operation, final ReadConcern readConcern) {
        if (!Pradar.isClusterTest()) {
            return ptOperationExecutor.execute(operation, readConcern);
        }
        Integer flag = MongoClientHolder.mongoIntegerMap.get(originator);
        if (flag == null) {//什么都没配置
            throw new PressureMeasureError("mongodb未配置影子库表");
        } else if (flag == 1) {//影子表
            WriteOperation<T> ptOperation = (WriteOperation<T>) MongoOperationUtil.generateWriteOperationWithShadowTable(operation, (Mongo) originator);
            return ptOperationExecutor.execute(ptOperation, readConcern);
        }
        {//影子库
            OperationExecutor operationExecutor = MongoClientHolder.mongoOperationExecutorMap.get(originator);
            WriteOperation<T> ptOperation = (WriteOperation<T>) MongoOperationUtil.generateWriteOperationWithShadowDb(operation, (Mongo) originator);
            return operationExecutor.execute(ptOperation, readConcern);
        }
    }

    @Override
    public <T> T execute(final ReadOperation<T> operation, final ReadPreference readPreference, final ReadConcern readConcern,
                         @Nullable final ClientSession session) {
        if (!Pradar.isClusterTest()) {
            return ptOperationExecutor.execute(operation, readPreference, readConcern, session);
        }
        Integer flag = MongoClientHolder.mongoIntegerMap.get(originator);
        if (flag == 1) {//影子表
            String businessCollection = ((FindOperation) operation).getNamespace().getCollectionName();
            Map<String, String> tableMapping = MongoClientHolder.mongoTableMappingMap.get(originator);
            //没有配置，直接查业务
//            if (tableMapping == null || tableMapping.size() == 0 || StringUtils.isBlank(tableMapping.get(businessCollection))) {
//                return ptOperationExecutor.execute(operation, readPreference, readConcern, session);
//            }
            MongoNamespace ptNamespace = new MongoNamespace(((FindOperation) operation).getNamespace().getDatabaseName(), Pradar.addClusterTestPrefix(((FindOperation) operation).getNamespace().getCollectionName()));
            ReadOperation<T> ptOperation = MongoOperationUtil.generateFromFindOperation((FindOperation) operation, ptNamespace);
            return ptOperationExecutor.execute(ptOperation, readPreference, readConcern, session);
        } else if (flag == 2) {//影子库
            OperationExecutor operationExecutor = MongoClientHolder.mongoOperationExecutorMap.get(originator);
            MongoNamespace ptNamespace = new MongoNamespace(Pradar.addClusterTestPrefix(((FindOperation) operation).getNamespace().getDatabaseName()), ((FindOperation) operation).getNamespace().getCollectionName());
            ReadOperation<T> ptOperation = MongoOperationUtil.generateFromFindOperation((FindOperation) operation, ptNamespace);
            return operationExecutor.execute(ptOperation, readPreference, readConcern);
        } else { //什么都没配置 读取直接查业务
            return ptOperationExecutor.execute(operation, readPreference, readConcern, session);
        }
    }

    @Override
    public <T> T execute(final WriteOperation<T> operation, final ReadConcern readConcern, @Nullable final ClientSession session) {
        if (!Pradar.isClusterTest()) {
            return ptOperationExecutor.execute(operation, readConcern, session);
        }
        Integer flag = MongoClientHolder.mongoIntegerMap.get(originator);
        if (flag == null) {//什么都没配置
            throw new PressureMeasureError("mongodb未配置影子库表");
        } else if (flag == 1) {//影子表
            WriteOperation<T> ptOperation = (WriteOperation<T>) MongoOperationUtil.generateWriteOperationWithShadowTable(operation, (Mongo) originator);
            return ptOperationExecutor.execute(ptOperation, readConcern, session);
        }
        {//影子库
            OperationExecutor operationExecutor = MongoClientHolder.mongoOperationExecutorMap.get(originator);
            WriteOperation<T> ptOperation = (WriteOperation<T>) MongoOperationUtil.generateWriteOperationWithShadowDb(operation, (Mongo) originator);
            return operationExecutor.execute(ptOperation, readConcern, session);
        }
    }
}
