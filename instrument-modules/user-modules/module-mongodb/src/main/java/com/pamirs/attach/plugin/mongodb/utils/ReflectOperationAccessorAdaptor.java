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

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.MongoNamespace;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/06/30 3:52 下午
 */
public class ReflectOperationAccessorAdaptor implements OperationAccessor {

    private static Map<Object, Field> objectFieldMap = new ConcurrentHashMap<Object, Field>(32, 1);

    private final boolean isRead;

    public ReflectOperationAccessorAdaptor(boolean isRead) {this.isRead = isRead;}

    @Override
    public boolean isRead() {
        return isRead;
    }

    @Override
    public MongoNamespace getMongoNamespace(Object operation) throws Exception {
        Class operationClass = operation.getClass();
        Field nameSpaceField = getField(operationClass);
        return (MongoNamespace)nameSpaceField.get(operation);
    }

    @Override
    public void setMongoNamespace(Object operation, MongoNamespace ptMongoNamespace) throws Exception {
        Class operationClass = operation.getClass();
        Field nameSpaceField = getField(operationClass);
        nameSpaceField.set(operation, ptMongoNamespace);
    }

    private Field getField(Class operationClass) throws NoSuchFieldException {
        Field nameSpaceField = objectFieldMap.get(operationClass);
        if (nameSpaceField == null) {
            synchronized (objectFieldMap) {
                nameSpaceField = objectFieldMap.get(operationClass);
                if (nameSpaceField == null) {
                    try {
                        nameSpaceField = operationClass.getDeclaredField("namespace");
                    } catch (NoSuchFieldException e) {
                        nameSpaceField = operationClass.getSuperclass().getDeclaredField("namespace");
                    }
                    nameSpaceField.setAccessible(true);
                    objectFieldMap.put(operationClass, nameSpaceField);
                }
            }
        }
        return nameSpaceField;
    }
}
