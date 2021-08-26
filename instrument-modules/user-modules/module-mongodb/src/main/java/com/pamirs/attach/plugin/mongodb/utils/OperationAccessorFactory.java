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
package com.pamirs.attach.plugin.mongodb.utils;

import com.mongodb.MongoNamespace;
import com.mongodb.operation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/30 4:28 下午
 */
public class OperationAccessorFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(OperationAccessorFactory.class.getName());

    private static Map<Class, OperationAccessor> operationNumMap = new HashMap<Class, OperationAccessor>(32, 1);

    static Field mongoNamespace;

    static {

        try {
            ReflectOperationAccessorAdaptor readReflectOperationAccessorAdaptor = new ReflectOperationAccessorAdaptor(true);

            //读操作
            operationNumMap.put(FindOperation.class, new ReflectOperationAccessorAdaptor(true) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((FindOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(CountOperation.class, readReflectOperationAccessorAdaptor);
            operationNumMap.put(DistinctOperation.class, readReflectOperationAccessorAdaptor);
            operationNumMap.put(GroupOperation.class, new ReflectOperationAccessorAdaptor(true) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((GroupOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(ListIndexesOperation.class, readReflectOperationAccessorAdaptor);
            operationNumMap.put(MapReduceWithInlineResultsOperation.class, new ReflectOperationAccessorAdaptor(true) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((MapReduceWithInlineResultsOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(ParallelCollectionScanOperation.class, readReflectOperationAccessorAdaptor);

            //写操作
            operationNumMap.put(MixedBulkWriteOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((MixedBulkWriteOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(BaseFindAndModifyOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((BaseFindAndModifyOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(BaseWriteOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((BaseWriteOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(FindAndDeleteOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((FindAndDeleteOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(FindAndReplaceOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((FindAndReplaceOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(FindAndUpdateOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((FindAndUpdateOperation) operation).getNamespace();
                }
            });
            operationNumMap.put(MapReduceToCollectionOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((MapReduceToCollectionOperation) operation).getNamespace();
                }
            });

            operationNumMap.put(DeleteOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((DeleteOperation) operation).getNamespace();
                }
            });

            operationNumMap.put(InsertOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((InsertOperation) operation).getNamespace();
                }
            });

            operationNumMap.put(UpdateOperation.class, new ReflectOperationAccessorAdaptor(false) {
                @Override
                public MongoNamespace getMongoNamespace(Object operation) {
                    return ((UpdateOperation) operation).getNamespace();
                }
            });

            if (mongoNamespace != null) {
                operationNumMap.put(DropCollectionOperation.class, new ReflectOperationAccessorAdaptor(false) {
                    @Override
                    public MongoNamespace getMongoNamespace(Object operation) {
                        try {
                            return (MongoNamespace) mongoNamespace.get(operation);
                        } catch (IllegalAccessException e) {
                            LOGGER.error("getMongoNamespace error {}", e);
                            return null;
                        }
                    }
                });
            }


            if (OperationAccessorFactory.class.getClassLoader().loadClass("com.mongodb.operation.DropCollectionOperation") != null) {
                try {
                    mongoNamespace = DropCollectionOperation.class.getDeclaredField("namespace");
                    mongoNamespace.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    LOGGER.error("get mongoNamespace field error {}", e);
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("mongodb class load fail : {} , load other mongodb version!", e.getMessage());
        } catch (NoClassDefFoundError error) {
            LOGGER.info("mongodb class load fail : {} , load other mongodb version!", error.getMessage());
        }
    }

    public static OperationAccessor getOperationAccessor(Object operation) {
        return operationNumMap.get(operation);
    }

    public static void destroy() {
        operationNumMap.clear();
    }
}
