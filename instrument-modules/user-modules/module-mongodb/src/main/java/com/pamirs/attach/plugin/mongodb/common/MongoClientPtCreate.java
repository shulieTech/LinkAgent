package com.pamirs.attach.plugin.mongodb.common;

/**
 * @author angju
 * @date 2020/9/7 12:43
 */
public class MongoClientPtCreate {
    public static ThreadLocal<Boolean> createPtMongoClient = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    public static void release() {
        createPtMongoClient = null;
    }
}
