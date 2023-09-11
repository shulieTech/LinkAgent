package com.pamirs.pradar;

import cn.hutool.core.collection.CollectionUtil;
import io.shulie.takin.sdk.kafka.HttpSender;
import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PradarRollingCollectorAppender extends PradarAppender {

    private static final Logger logger = LoggerFactory.getLogger(PradarRollingCollectorAppender.class);

    private final byte dataType;

    private final int version;

    private final String hostAddress;

    StringBuilder stringBuilder = new StringBuilder();

    List<Object> sendContent = new ArrayList<>(1000);

    private MessageSendService messageSendService;

    public PradarRollingCollectorAppender(byte dataType, int version) {
        this.hostAddress = System.getProperty("pradar.data.pusher.pinpoint.collector.address");
        this.dataType = dataType;
        this.version = version;
        this.messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();
    }


    @Override
    public void appendObject(Object log) {
        if (log instanceof String) {
            this.append((String) log);
        } else {
            sendContent.add(log);
        }
    }

    private void send(String log) {
        messageSendService.send(dataType, version, log, hostAddress, new MessageSendCallBack() {
            @Override
            public void success() {
            }

            @Override
            public void fail(String errorMessage) {
            }
        });
    }

    private void send(List<Object> logs){
        logs.forEach(log -> {
            messageSendService.send(log, new MessageSendCallBack() {
                @Override
                public void success() {
                }

                @Override
                public void fail(String errorMessage) {
                }
            }, new HttpSender() {
                @Override
                public void sendMessage() {

                }
            });
        });
    }

    @Override
    public void append(String log) {
        stringBuilder.append(log);
    }

    @Override
    public void flush() {
        if (stringBuilder.length() > 0){
            this.send(stringBuilder.toString());
            stringBuilder = new StringBuilder();
        }

        if (CollectionUtil.isNotEmpty(sendContent)){
            List<Object> batch = null;
            synchronized (this) {
                batch = sendContent;
                sendContent = new ArrayList<>(1000);
            }
            this.send(batch);
        }
    }

    @Override
    public void rollOver() {

    }

    @Override
    public void reload() {

    }

    @Override
    public void close() {
        messageSendService.stop();
    }

    @Override
    public void cleanup() {

    }
}
