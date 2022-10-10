package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.mongo.MongoDataSourceFetcher;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowMongoPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MongoPreCheckCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoPreCheckCommandProcessor.class.getName());

    public static void processPreCheckCommand(String commandId, final JdbcPreCheckCommand command, final Consumer<CommandAck> callback) {
        MongoDataSourceFetcher.refreshDataSources();

        CommandAck ack = new CommandAck();
        ack.setCommandId(commandId);
        CommandExecuteResult result = new CommandExecuteResult();

        DataSourceEntity bizDataSource = command.getBizDataSource();
        Object client = MongoDataSourceFetcher.getBizClient(bizDataSource.getUrl());
        if (client == null) {
            LOGGER.error("[shadow-preparation] can`t find business mongo client object instance for url:{}", bizDataSource.getUrl());
            result.setSuccess(false);
            result.setResponse("mongodb业务数据源没有对象实例,请先发业务流量触发业务数据源实例化");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        boolean isMongoV4 = MongoDataSourceFetcher.isMongoV4();
        Integer shadowType = command.getShadowType();
        Integer dsType = shadowType == 1 ? 0 : shadowType == 2 ? 2 : shadowType == 3 ? 1 : null;
        String shadowUrl = command.getShadowDataSource() == null ? null : command.getShadowDataSource().getUrl();

        if (dsType == null) {
            LOGGER.error("[shadow-preparation] illegal shadow type:{} for url:{}", shadowType, bizDataSource.getUrl());
            result.setSuccess(false);
            result.setResponse("隔离类型不合法,只能为 0，1，2，3");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        if(dsType == 1 && CollectionUtils.isEmpty(command.getTables())){
            LOGGER.error("[shadow-preparation] ds type :{} must assign shadow tables", dsType);
            result.setSuccess(false);
            result.setResponse("隔离类型为影子表时必须指定业务表名称");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        if ((dsType == 0 || dsType == 2) && (command.getShadowDataSource() == null || command.getShadowDataSource().getUrl() == null)) {
            LOGGER.error("[shadow-preparation] ds type :{} must assign shadow url", dsType);
            result.setSuccess(false);
            result.setResponse("隔离类型为影子库或影子库影子表时必须指定影子url");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);

        ShadowMongoPreCheckEvent event = new ShadowMongoPreCheckEvent(isMongoV4, dsType, bizDataSource.getUrl(), shadowUrl, command.getTables(), client, latch);
        EventRouter.router().publish(event);

        try {
            boolean handler = latch.await(30, TimeUnit.SECONDS);
            if (!handler) {
                LOGGER.error("[shadow-preparation] publish ShadowMongoPreCheckEvent after 30s has not accept result!");
            }
            result.setSuccess("success".equals(event.getResult()));
            result.setResponse(event.getResult());
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
        } catch (InterruptedException e) {
            LOGGER.error("[shadow-preparation] wait for mongo precheck processing occur exception", e);
        }

    }
}
