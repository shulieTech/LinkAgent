package com.pamirs.attach.plugin.mongodb.listener;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.SyncObjectService;
import com.pamirs.pradar.bean.SyncObject;
import com.pamirs.pradar.bean.SyncObjectData;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowMongoPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MongoDBShadowPreCheckEventListener implements PradarEventListener {

    private static String mongo_datasource_sync_key = "com.mongodb.client.internal.MongoClientImpl";

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoDBShadowPreCheckEventListener.class.getName());

    /**
     * 临时创建的客户端对象需要被关闭
     */
    private Set<MongoClient> needClosedClient = new HashSet<MongoClient>();

    @Override
    public EventResult onEvent(IEvent iEvent) {
        if (!(iEvent instanceof ShadowMongoPreCheckEvent)) {
            return EventResult.IGNORE;
        }
        ShadowMongoPreCheckEvent event = (ShadowMongoPreCheckEvent) iEvent;
        boolean isV4 = event.isV4();
        if (isV4) {
            return EventResult.IGNORE;
        }
        Thread.currentThread().setContextClassLoader(event.getMongoClient().getClass().getClassLoader());

        switch (event.getDsType()) {
            case 0:
                validateShadowDatabaseConfig(event, false);
                break;
            case 1:
                validateShadowTableConfig(event);
                break;
            case 2:
                validateShadowDatabaseConfig(event, true);
                break;
        }
        if (event.getResult() == null) {
            event.handlerResult("success");
        }
        closeShadowClient();
        return EventResult.success(iEvent.getTarget());
    }

    /**
     * 校验影子库模式配置
     *
     * @param event
     * @param shadowTable 影子表
     * @return
     */
    private void validateShadowDatabaseConfig(ShadowMongoPreCheckEvent event, boolean shadowTable) {
        MongoDatabase bizDatabase = getDatabase(event.getBizUrl(), (MongoClient) event.getMongoClient(), event);
        if (bizDatabase == null) {
            return;
        }
        MongoDatabase shadowDatabase = getDatabase(event.getShadowUrl(), null, event);
        if (shadowDatabase == null) {
            return;
        }
        List<String> tables = event.getTables() == null ? new ArrayList<String>() : event.getTables();
        if (tables.isEmpty()) {
            for (String collectionName : bizDatabase.listCollectionNames()) {
                tables.add(collectionName);
            }
        }
        for (String table : tables) {
            boolean validated = isCollectionValidated(shadowDatabase, shadowTable ? Pradar.addClusterTestPrefix(table) : table, event);
            if (!validated) {
                return;
            }
        }
    }

    /**
     * 校验影子表配置
     *
     * @param event
     */
    private void validateShadowTableConfig(ShadowMongoPreCheckEvent event) {
        MongoDatabase database = getDatabase(event.getBizUrl(), (MongoClient) event.getMongoClient(), event);
        for (String table : event.getTables()) {
            boolean validated = isCollectionValidated(database, table, event);
            if (!validated) {
                return;
            }
            validated = isCollectionValidated(database, Pradar.addClusterTestPrefix(table), event);
            if (!validated) {
                return;
            }
        }
    }

    private boolean isCollectionValidated(MongoDatabase database, String table, ShadowMongoPreCheckEvent event) {
        try {
            database.getCollection(table);
            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.error("[Mongodb]: shadow collection {} not exists!", table);
            event.handlerResult(String.format("影子表%s不存在", table));
            return false;
        } catch (Exception e) {
            LOGGER.error("[Mongodb]: get collection {} occur exception", table, e);
            event.handlerResult(String.format("获取影子表%s出现异常,异常信息:%s", table, e.getMessage()));
            return false;
        }
    }

    private MongoDatabase getDatabase(String url, MongoClient client, ShadowMongoPreCheckEvent event) {
        MongoClient mongoClient = client;
        if (mongoClient == null) {
            mongoClient = MongoClients.create(url);
            needClosedClient.add(mongoClient);
        }
        String database = url.substring(url.lastIndexOf("/") + 1);
        if (StringUtils.isEmpty(database)) {
            LOGGER.error("[Mongodb]: url:{} not assign database", url);
            event.handlerResult(String.format("配置url%s中未指定数据库", url));
            return null;
        }
        try {
            return mongoClient.getDatabase(database);
        } catch (IllegalArgumentException e) {
            LOGGER.error("[Mongodb]: database {} name is illegal!", database);
            event.handlerResult(String.format("数据库%s名称不符合规范", database));
        } catch (Exception e) {
            LOGGER.error("[Mongodb]: get database {} occur exception", database, e);
            event.handlerResult(String.format("查询数据库%s出现异常,异常信息:%s", database, e.getMessage()));
        }
        return null;
    }

    private void closeShadowClient() {
        SyncObject syncObject = SyncObjectService.removeSyncObject(mongo_datasource_sync_key);
        Iterator<SyncObjectData> iterator = syncObject.getDatas().iterator();
        while (iterator.hasNext()) {
            SyncObjectData next = iterator.next();
            Object target = next.getTarget();
            if (needClosedClient.contains(target)) {
                iterator.remove();
            }
        }
        for (MongoClient mongoClient : needClosedClient) {
            mongoClient.close();
        }
    }

    @Override
    public int order() {
        return 24;
    }
}
