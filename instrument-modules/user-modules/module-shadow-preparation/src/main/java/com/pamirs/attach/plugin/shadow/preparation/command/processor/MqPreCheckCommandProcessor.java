package com.pamirs.attach.plugin.shadow.preparation.command.processor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pamirs.attach.plugin.shadow.preparation.command.CommandExecuteResult;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowMqPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.preparation.ShadowSfKafkaPreCheckEvent;
import com.pamirs.pradar.pressurement.agent.shared.service.EventRouter;
import io.shulie.agent.management.client.model.Command;
import io.shulie.agent.management.client.model.CommandAck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MqPreCheckCommandProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(MqPreCheckCommandProcessor.class.getName());

    public static void processPreCheckCommand(Command command, Consumer<CommandAck> callback) {
        CommandAck ack = new CommandAck();
        ack.setCommandId(command.getId());
        CommandExecuteResult result = new CommandExecuteResult();

        String content = command.getArgs();
        LOGGER.info("[shadow-preparation] accept shadow mq precheck command, content:{}", content);

        JSONArray mapList = JSON.parseArray(content);
        if (mapList.isEmpty()) {
            LOGGER.error("[shadow-preparation] accept shadow mq precheck command with empty content, ignore!");
            result.setSuccess(false);
            result.setResponse("拉取到的命令为空");
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
        }

        CountDownLatch latch = new CountDownLatch(mapList.size());
        List<IEvent> events = new ArrayList<>();

        for (Object o : mapList) {
            JSONObject obj = (JSONObject) o;
            String type = obj.getString("type");
            // 有type,开源的配置
            if (type != null) {
                Map<String, List<String>> topicGroups = (Map<String, List<String>>) obj.get("topicGroups");
                ShadowMqPreCheckEvent event = new ShadowMqPreCheckEvent(type, topicGroups, latch);
                events.add(event);
                EventRouter.router().publish(event);
                continue;
            }
            // sf-kafka配置
            boolean isSfKafkaConfig = obj.containsKey("topicTokens") || obj.containsKey("systemIdToken");
            if (isSfKafkaConfig) {
                String sfKafkaConfig = ((JSONObject) o).toJSONString();
                ShadowSfKafkaPreCheckEvent event = JSON.parseObject(sfKafkaConfig, ShadowSfKafkaPreCheckEvent.class);
                event.setLatch(latch);
                events.add(event);
                EventRouter.router().publish(event);
            }
        }

        try {
            boolean handler = latch.await(30, TimeUnit.SECONDS);
            if (!handler) {
                LOGGER.error("[shadow-preparation] publish ShadowMqPreCheckEvent after 30s still not accept result!");
            }
            final AtomicBoolean success = new AtomicBoolean(true);
            String commandResult = events.stream().map(new Function<IEvent, String>() {
                @Override
                public String apply(IEvent event) {
                    if (event instanceof ShadowMqPreCheckEvent) {
                        return ((ShadowMqPreCheckEvent) event).getCheckResult().entrySet().stream().map(new Function<Map.Entry<String, String>, String>() {
                            @Override
                            public String apply(Map.Entry<String, String> entry) {
                                // 当某个topic-group验证不通过
                                if (success.get() && !"success".equals(entry.getValue())) {
                                    success.set(false);
                                }
                                return entry.getKey() + ">>" + entry.getValue();
                            }
                        }).collect(Collectors.joining(";\n"));
                    } else {
                        return ((ShadowSfKafkaPreCheckEvent) event).getResult();
                    }

                }
            }).collect(Collectors.joining(";\n"));

            result.setSuccess(success.get());
            result.setResponse(commandResult);
            ack.setResponse(JSON.toJSONString(result));
            callback.accept(ack);
        } catch (InterruptedException e) {
            LOGGER.error("[shadow-preparation] wait for mq precheck processing occur exception", e);
        }
    }

}
