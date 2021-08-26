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
package com.pamirs.pradar.pressurement.datasource.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xiaobin.zfb|xiaobin@shulie.io
 * @since 2021/7/5 3:33 下午
 */
public class SqlMetadataParser {
    private final static Logger logger = LoggerFactory.getLogger(SqlMetadataParser.class);

    private static ConcurrentMap<String, SqlMetaData> sqlMetaDatas = new ConcurrentHashMap<String, SqlMetaData>();

    public static SqlMetaData parse(DbType dbType, String url) {
        try {
            SqlMetaData sqlMetaData = sqlMetaDatas.get(url);
            if (sqlMetaData != null) {
                SqlMetaData metaData = new SqlMetaData();
                metaData.setHost(sqlMetaData.getHost());
                metaData.setPort(sqlMetaData.getPort());
                metaData.setDbType(sqlMetaData.getDbType());
                metaData.setDbName(sqlMetaData.getDbName());
                metaData.setUrl(sqlMetaData.getUrl());
                metaData.setSql(sqlMetaData.getSql());
                return metaData;
            }
            sqlMetaData = dbType.readMetaData(url);
            if (sqlMetaData == null) {
                return null;
            }
            SqlMetaData old = sqlMetaDatas.putIfAbsent(url, sqlMetaData);
            if (old != null) {
                sqlMetaData = old;
            }

            SqlMetaData metaData = new SqlMetaData();
            metaData.setHost(sqlMetaData.getHost());
            metaData.setPort(sqlMetaData.getPort());
            metaData.setDbType(sqlMetaData.getDbType());
            metaData.setDbName(sqlMetaData.getDbName());
            metaData.setUrl(sqlMetaData.getUrl());
            metaData.setSql(sqlMetaData.getSql());
            return metaData;
        } catch (Throwable e) {
            logger.error("Sql metadata parse err. maybe db type is not supported. dbType={}, url={}", dbType, url, e);
            return null;
        }
    }

    public static void clear() {
        sqlMetaDatas.clear();
    }
}
