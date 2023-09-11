package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.pamirs.attach.plugin.shadow.preparation.command.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.command.JdbcPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.commons.CommandAck;
import com.pamirs.attach.plugin.shadow.preparation.jdbc.entity.DataSourceEntity;
import com.pamirs.attach.plugin.shadow.preparation.mongo.MongoClientsFetcher;
import com.pamirs.pradar.gson.GsonFactory;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowMongoPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.shulie.instrument.simulator.api.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.pamirs.pradar.gson.GsonFactory.getGson;

public class MongoPreCheckCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoPreCheckCommandProcessor.class.getName());

    public static void processPreCheckCommand(String commandId, final JdbcPreCheckCommand command, final Consumer<CommandAck> callback) {
        MongoClientsFetcher.refreshClients();

        CommandAck ack = new CommandAck();
        ack.setCommandId(commandId);
        CommandExecuteResult result = new CommandExecuteResult();

        DataSourceEntity bizDataSource = command.getBizDataSource();
        Object client = MongoClientsFetcher.getBizClient(bizDataSource.getUrl());
        if (client == null) {
            LOGGER.error("[shadow-preparation] can`t find business mongo client object instance for url:{}", bizDataSource.getUrl());
            result.setSuccess(false);
            result.setResponse("mongodb业务数据源没有对象实例,请先发业务流量触发业务数据源实例化");
            ack.setResponse(getGson().toJson(result));
            callback.accept(ack);
            return;
        }

        boolean isMongoV4 = MongoClientsFetcher.isMongoV4();
        Integer dsType = command.getShadowType();
        String shadowUrl = command.getShadowDataSource() == null ? null : command.getShadowDataSource().getUrl();

        if (dsType == null) {
            LOGGER.error("[shadow-preparation] illegal shadow type:{} for url:{}", dsType, bizDataSource.getUrl());
            result.setSuccess(false);
            result.setResponse("隔离类型不合法,只能为 0，1，2，3");
            ack.setResponse(GsonFactory.getGson().toJson(result));
            callback.accept(ack);
            return;
        }

        if(dsType == 1 && CollectionUtils.isEmpty(command.getTables())){
            LOGGER.error("[shadow-preparation] ds type :{} must assign shadow tables", dsType);
            result.setSuccess(false);
            result.setResponse("隔离类型为影子表时必须指定业务表名称");
            ack.setResponse(GsonFactory.getGson().toJson(result));
            callback.accept(ack);
            return;
        }

        if ((dsType == 0 || dsType == 2) && (command.getShadowDataSource() == null || command.getShadowDataSource().getUrl() == null)) {
            LOGGER.error("[shadow-preparation] ds type :{} must assign shadow url", dsType);
            result.setSuccess(false);
            result.setResponse("隔离类型为影子库或影子库影子表时必须指定影子url");
            ack.setResponse(GsonFactory.getGson().toJson(result));
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
            ack.setResponse(GsonFactory.getGson().toJson(result));
            callback.accept(ack);
        } catch (InterruptedException e) {
            LOGGER.error("[shadow-preparation] wait for mongo precheck processing occur exception", e);
        }

    }
}
