package com.pamirs.attach.plugin.mongodb.common;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.internal.OperationExecutor;

/**
 * @author angju
 * @date 2020/8/7 19:47
 */
public class MongoClientHolder {
    public static Map<String, MongoClient> mongoClientMap = new HashMap<String, MongoClient>();

    /**
     * 高版本业务库和影子库的映射
     */
    public static Map<Mongo, OperationExecutor> mongoOperationExecutorMap = new HashMap<Mongo, OperationExecutor>(2, 2);

    /**
     * 3.2.2版本 影子库和业务库的映射
     */
    public static Map<MongoClient, MongoClient> mongoClientMongoClientMap = new HashMap<MongoClient, MongoClient>(1, 1);

    public static Map<Mongo, Integer> mongoIntegerMap = new HashMap<Mongo, Integer>(2, 1);
    public static Map<Mongo, Map<String, String>> mongoTableMappingMap = new HashMap<Mongo, Map<String, String>>(2, 1);

    public static ThreadLocal<Mongo> mongoHolder = new ThreadLocal<Mongo>() {
        @Override
        protected Mongo initialValue() {
            return null;
        }
    };

    public static void release() {
        mongoClientMap.clear();
        mongoOperationExecutorMap.clear();
        mongoClientMongoClientMap.clear();
        mongoIntegerMap.clear();
        mongoTableMappingMap.clear();
        mongoHolder = null;
    }

}
