package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.pamirs.attach.plugin.shadow.preparation.command.EsConfigPushCommand;
import com.pamirs.attach.plugin.shadow.preparation.es.EsClientFetcher;
import com.pamirs.attach.plugin.shadow.preparation.es.EsConfigEntity;
import com.pamirs.attach.plugin.shadow.preparation.commons.Config;
import com.pamirs.attach.plugin.shadow.preparation.commons.ConfigAck;
import com.pamirs.pradar.internal.config.ShadowEsServerConfig;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowEsActiveEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowEsDisableEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import com.pamirs.pradar.pressurement.agent.shared.service.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EsConfigPushCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(EsConfigPushCommandProcessor.class.getName());

    public static void processConfigPushCommand(final Config config, final Consumer<ConfigAck> callback) {
        LOGGER.info("[shadow-preparation] accept shadow es push command, content:{}", config.getParam());

        EsClientFetcher.refreshClients();

        ConfigAck ack = new ConfigAck();
        ack.setType(config.getType());
        ack.setVersion(config.getVersion());

        EsConfigPushCommand cmd;
        try {
            cmd = JSON.parseObject(config.getParam(), EsConfigPushCommand.class);
        } catch (Exception e) {
            LOGGER.error("[shadow-preparation] parse es config push command occur exception", e);
            ack.setResultCode(500);
            ack.setResultDesc("解析数据源下发命令失败");
            callback.accept(ack);
            return;
        }

        Object[] result = compareShadowDataSource(cmd.getData());
        Set<String> needClosed = (Set<String>) result[0];
        Set<EsConfigEntity> needAdd = (Set<EsConfigEntity>) result[1];

        List<ShadowEsServerConfig> configs = toEsServerConfig(cmd.getData());
        GlobalConfig.getInstance().setShadowEsServerConfigs(toMap(configs));

        CountDownLatch latch = new CountDownLatch(1);
        if (!needClosed.isEmpty()) {
            EsClientFetcher.removeShadowClients(needClosed);
            EventRouter.router().publish(new ShadowEsDisableEvent(needClosed, EsClientFetcher.getBizClassLoader(), latch));
            try {
                boolean handler = latch.await(30, TimeUnit.SECONDS);
                if (!handler) {
                    LOGGER.error("[shadow-preparation] publish shadow es disable event after 30s has not been processed");
                }
            } catch (InterruptedException e) {
            }
        }


        StringBuilder sb = new StringBuilder();
        List<ShadowEsActiveEvent> events = new ArrayList<>();

        if (needAdd.isEmpty()) {
            LOGGER.info("[shadow-preparation] don`t exists need active shadow config, es shadow config active success");
            ack.setResultCode(500);
            callback.accept(ack);
            return;
        }

        latch = new CountDownLatch(needAdd.size());
        for (EsConfigEntity esConfig : needAdd) {
            Object bizClient = EsClientFetcher.getBizClient(esConfig.getBusinessNodes());
            if (bizClient == null) {
                LOGGER.error("[shadow-preparation] can`t find biz es client for business nodes:{}", esConfig.getBusinessNodes());
                sb.append(String.format("找不到node:%s的client对象,请核对nodes是否配置正确", esConfig.getBusinessNodes()));
                continue;
            }
            ShadowEsActiveEvent event = new ShadowEsActiveEvent(bizClient, esConfig.getBusinessNodes(), latch);
            EventRouter.router().publish(event);
            events.add(event);
        }

        try {
            boolean handler = latch.await(30, TimeUnit.SECONDS);
            if (!handler) {
                LOGGER.error("[shadow-preparation] publish shadow es active event after 30s has not been processed");
            }
            for (ShadowEsActiveEvent event : events) {
                String ret = event.getResult();
                if (!"success".equals(ret)) {
                    sb.append(ret);
                }
            }
            if (sb.length() > 0) {
                ack.setResultCode(500);
                ack.setResultDesc(sb.toString());
            } else {
                ack.setResultCode(500);
            }
            callback.accept(ack);
        } catch (Exception e) {
        }
    }

    /**
     * 返回新增的和需要陪关闭的影子数据源
     *
     * @param data
     * @return
     */
    private static Object[] compareShadowDataSource(List<EsConfigEntity> data) {
        // 需要被关闭的影子数据源
        Set<String> needClosed = new HashSet<String>(EsClientFetcher.getShadowKeys());
        // 新增的数据源
        Set<EsConfigEntity> needAdd = new HashSet<>();

        for (EsConfigEntity config : data) {
            // 影子表模式直接关闭
            if (config.getShadowType() == 3) {
                continue;
            }
            String nodes = Arrays.stream(config.getPerformanceTestNodes().split(",")).sorted().collect(Collectors.joining(","));
            // 当前影子数据源存在
            if (needClosed.remove(nodes)) {
                continue;
            }
            needAdd.add(config);
        }
        // 遇到特殊情况, 多个业务数据源的影子数据源是一样的, 需要禁用其中一个
        if (needClosed.isEmpty() && data.size() < EsClientFetcher.getShadowClientNum()) {
            // 因为没有保存业务数据源和影子数据源的映射关系，所以清除所有影子数据源，重新构建
            return new Object[]{EsClientFetcher.getShadowKeys(), data.stream().filter(esConfigEntity -> esConfigEntity.getShadowType() == 1).collect(Collectors.toSet())};
        }
        return new Object[]{needClosed, needAdd};
    }

    private static List<ShadowEsServerConfig> toEsServerConfig(List<EsConfigEntity> entities) {
        List<ShadowEsServerConfig> configs = new ArrayList<>();
        for (EsConfigEntity entity : entities) {
            ShadowEsServerConfig config = new ShadowEsServerConfig(
                    Arrays.asList(entity.getBusinessNodes().split(",")),
                    Arrays.asList(entity.getPerformanceTestNodes().split(",")),
                    entity.getBusinessClusterName(), entity.getPerformanceClusterName(), entity.getPtUserName(), entity.getPtPassword());
            configs.add(config);
        }
        return configs;
    }

    private static Map<String, ShadowEsServerConfig> toMap(List<ShadowEsServerConfig> list) {
        Map<String, ShadowEsServerConfig> globalConfig = new HashMap<>();
        for (ShadowEsServerConfig config : list) {
            globalConfig.put(config.identifyKey(), config);
        }
        return globalConfig;
    }

}
