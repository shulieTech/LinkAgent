package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.CommandExecuteResult;
import com.pamirs.attach.plugin.shadow.preparation.command.EsPreCheckCommand;
import com.pamirs.attach.plugin.shadow.preparation.es.EsClientFetcher;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowEsPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EsPreCheckCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(EsPreCheckCommandProcessor.class.getName());

    public static void processPreCheckCommand(Command command, Consumer<CommandAck> callback) {
        LOGGER.info("[shadow-preparation] accept shadow es precheck command, content:{}", command.getArgs());

        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        EsPreCheckCommand entity;
        try {
            entity = JSON.parseObject(command.getArgs(), EsPreCheckCommand.class);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] parse es precheck command occur exception", e);
            result.setSuccess(false);
            result.setResponse("解析校验命令失败,配置格式不正确");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        EsClientFetcher.refreshClients();

        // 0-未设置 1-影子库 2-影子库/影子表 3-影子表
        Integer shadowType = entity.getShadowType();

        if (shadowType == 1 && entity.getPerformanceTestNodes() == null) {
            LOGGER.error("[shadow-preparation] ds type is shadow database or shadow database table, but shadow nodes is null");
            result.setSuccess(false);
            result.setResponse("影子库/影子库影子表模式时影子节点不能为空");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        String businessNodes = entity.getBusinessNodes();
        Object bizClient = EsClientFetcher.getBizClient(businessNodes);
        if (bizClient == null) {
            LOGGER.error("[shadow-preparation] can`t find elasticsearch business client with nodes:{}", businessNodes);
            result.setSuccess(false);
            result.setResponse(String.format("应用内部找不到node:%s的客户端对象,请确保业务nodes配置正确", businessNodes));
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        ShadowEsPreCheckEvent event = new ShadowEsPreCheckEvent(bizClient, entity.getShadowType(), entity.getIndices(),
                entity.getBusinessNodes(), entity.getPerformanceTestNodes(),
                entity.getBusinessClusterName(), entity.getPerformanceClusterName(), entity.getPtUserName(), entity.getPtPassword(), latch);
        EventRouter.router().publish(event);

        try {
            boolean handler = latch.await(30, TimeUnit.SECONDS);
            if (!handler) {
                LOGGER.error("[shadow-preparation] publish shadow es pre check event after 30s has not been processed");
            }

            if ("success".equals(event.getResult())) {
                result.setSuccess(true);
            } else {
                result.setSuccess(false);
                result.setResponse(event.getResult());
            }
            callback.accept(ack);
        } catch (Exception e) {

        }

    }

}
