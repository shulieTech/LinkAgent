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
package com.pamirs.attach.plugin.mongodb;

import com.pamirs.pradar.MiddlewareType;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/7/13 9:14 下午
 */
public class MongodbConstants {
    public final static String PLUGIN_NAME = "mongodb";
    public final static int PLUGIN_TYPE = MiddlewareType.TYPE_DB;

    public final static String[] mongoCollectionList = new String[]{"count",
        "countDocuments",
        "estimatedDocumentCount",
        "distinct",
        "find",
        "aggregate",
        "watch",
        "mapReduce",
        "bulkWrite",
        "insertOne",
        "insertMany",
        "deleteOne",
        "deleteMany",
        "replaceOne",
        "updateMany",
        "findOneAndDelete",
        "findOneAndReplace",
        "findOneAndUpdate",
        "drop",
        "createIndex",
        "createIndexes",
        "listIndexes",
        "dropIndex",
        "dropIndexes",
        "renameCollection"};
}
