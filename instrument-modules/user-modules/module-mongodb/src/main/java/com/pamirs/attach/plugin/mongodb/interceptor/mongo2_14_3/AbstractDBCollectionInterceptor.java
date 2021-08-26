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
package com.pamirs.attach.plugin.mongodb.interceptor.mongo2_14_3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.pamirs.pradar.CutOffResult;
import com.pamirs.pradar.ErrorTypeEnum;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;
import com.pamirs.pradar.interceptor.CutoffInterceptorAdaptor;
import com.pamirs.pradar.internal.config.ShadowDatabaseConfig;
import com.pamirs.pradar.pressurement.agent.shared.service.ErrorReporter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import com.shulie.instrument.simulator.api.listener.ext.Advice;
import org.apache.commons.lang.StringUtils;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/05/20 4:21 下午
 */
public abstract class AbstractDBCollectionInterceptor extends CutoffInterceptorAdaptor {

    private final Map<String, MongoClient> clientMapping
        = new ConcurrentHashMap<String, MongoClient>();

    private final Map<String, DBCollection> collectionMapping
        = new ConcurrentHashMap<String, DBCollection>();

    private final Object lock = new Object();

    @Override
    public CutOffResult cutoff0(Advice advice) throws Throwable {
        if (!Pradar.isClusterTest()) {
            return CutOffResult.passed();
        }
        if (!check(advice)) {
            return CutOffResult.passed();
        }
        DBCollection dbCollection = (DBCollection)advice.getTarget();
        DBCollection ptCollection = getPtCollection(dbCollection);
        if (ptCollection == null) {
            return CutOffResult.passed();
        }
        return CutOffResult.cutoff(cutoffShadow(ptCollection, advice));
    }

    protected DBCollection getPtCollection(DBCollection bizDbCollection) throws Throwable {

        String busCollectionName = getCollectionName(bizDbCollection);

        if ("$cmd".equals(busCollectionName)) {
            return null;
        }

        if (Pradar.isClusterTestPrefix(busCollectionName) || Pradar.isClusterTestPrefix(
            bizDbCollection.getDB().getName())) {
            return null;
        }

        if (StringUtils.isBlank(busCollectionName)) {
            throw new PressureMeasureError("mongo压测请求获取业务collection异常");
        }

        DBCollection ptCollection = collectionMapping.get(busCollectionName);
        if (ptCollection == null) {
            ptCollection = collectionMapping.get(busCollectionName);
            synchronized (lock) {
                if (ptCollection == null) {
                    ShadowDatabaseConfig shadowDatabaseConfig = getShadowDatabaseConfig(bizDbCollection);
                    if (shadowDatabaseConfig == null) {
                        if (isRead()) {
                            //读操作，未配置影子表，直接读取业务表
                            return null;
                        } else {
                            ErrorReporter.buildError()
                                .setErrorType(ErrorTypeEnum.DataSource)
                                .setErrorCode("datasource-0002")
                                .setMessage("mongodb影子库/表未配置！")
                                .setDetail(
                                    "业务库配置:::url: " + bizDbCollection.getDB().getMongo().getAddress().toString())
                                .report();
                            throw new PressureMeasureError("mongodb影子库/表未配置");
                        }
                    }

                    if (shadowDatabaseConfig.isShadowDatabase()) {
                        ptCollection = doShadowDatabase(bizDbCollection, busCollectionName, shadowDatabaseConfig);
                    } else {
                        ptCollection = doShadowTable(bizDbCollection, busCollectionName, shadowDatabaseConfig);
                    }
                    if (ptCollection != null) {
                        ptCollection.setWriteConcern(bizDbCollection.getWriteConcern());
                        ptCollection.setDBDecoderFactory(bizDbCollection.getDBDecoderFactory());
                        ptCollection.setDBEncoderFactory(bizDbCollection.getDBEncoderFactory());
                        ptCollection.setObjectClass(bizDbCollection.getObjectClass());
                        ptCollection.setReadPreference(bizDbCollection.getReadPreference());
                        ptCollection.setOptions(bizDbCollection.getOptions());
                        collectionMapping.put(busCollectionName, ptCollection);
                    }
                }
            }
        }
        return ptCollection;
    }

    protected DBCollection doShadowDatabase(DBCollection dbCollection, String busCollectionName,
        ShadowDatabaseConfig config) throws Throwable {
        MongoClient ptMongoClient = getPtMongoClient(config);
        return ptMongoClient.getDB(Pradar.addClusterTestPrefix(dbCollection.getDB().getName())).getCollection(
            Pradar.addClusterTestPrefix(busCollectionName));
    }

    /**
     * 获取影子表时需要忽略配置与实际的大小写差异
     *
     * @param shadowDatabaseConfig 影子配置
     * @param bizTableName         业务表名
     * @return
     */
    private String getShadowTableName(ShadowDatabaseConfig shadowDatabaseConfig, String bizTableName) {
        if (shadowDatabaseConfig == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : shadowDatabaseConfig.getBusinessShadowTables().entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), bizTableName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    protected DBCollection doShadowTable(DBCollection dbCollection, String busCollectionName,
        ShadowDatabaseConfig config) throws Throwable {
        if (Pradar.isClusterTestPrefix(busCollectionName)) {
            return null;
        }
        String shadowTableName = getShadowTableName(config, busCollectionName);
        if (shadowTableName == null) {
            ErrorReporter.buildError()
                .setErrorType(ErrorTypeEnum.DataSource)
                .setErrorCode("datasource-0002")
                .setMessage("mongodb影子表未配置！")
                .setDetail("表名:" + busCollectionName)
                .report();
            throw new PressureMeasureError(String.format("mongodb影子表未配置 ： %s", busCollectionName));
        }

        return dbCollection.getDB().getCollection(
            shadowTableName);
    }

    protected abstract boolean check(Advice advice);

    protected abstract boolean isRead();

    protected abstract Object cutoffShadow(DBCollection ptDbCollection, Advice advice) throws Throwable;

    private MongoClient getPtMongoClient(ShadowDatabaseConfig config) {
        MongoClient mongoClient = clientMapping.get(config.getUrl());
        if (mongoClient == null) {
            synchronized (lock) {
                mongoClient = clientMapping.get(config.getUrl());
                if (mongoClient == null) {
                    mongoClient = new MongoClient(new MongoClientURI(config.getShadowUrl()));
                    clientMapping.put(config.getUrl(), mongoClient);
                }
            }
        }
        return mongoClient;
    }

    private ShadowDatabaseConfig getShadowDatabaseConfig(DBCollection dbCollection) {
        List<ServerAddress> serverAddressList = dbCollection.getDB().getMongo().getAllAddress();
        for (ShadowDatabaseConfig config : GlobalConfig.getInstance().getShadowDatasourceConfigs().values()) {
            for (ServerAddress serverAddress : serverAddressList) {
                if (config.getUrl().contains(serverAddress.toString())) {
                    return config;
                }
            }
        }
        return null;
    }

    /**
     * 获取业务collection
     */
    private String getCollectionName(DBCollection dbCollection) {
        return dbCollection.getFullName().split("\\.")[1];
    }

    @Override
    protected void clean() {
        Iterator<Map.Entry<String, MongoClient>> it = clientMapping.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, MongoClient> entry = it.next();
            it.remove();
            entry.getValue().close();
        }
        clientMapping.clear();
        collectionMapping.clear();
    }
}
