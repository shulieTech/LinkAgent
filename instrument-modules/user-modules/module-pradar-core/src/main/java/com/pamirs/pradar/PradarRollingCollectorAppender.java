package com.pamirs.pradar;

import io.shulie.takin.sdk.kafka.MessageSendCallBack;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceFactory;

public class PradarRollingCollectorAppender extends PradarAppender {

    private final byte dataType;

    private final int version;

    private final String hostAddress;

    StringBuilder stringBuilder = new StringBuilder();

    private MessageSendService messageSendService;

    public PradarRollingCollectorAppender(byte dataType, int version) {
        this.hostAddress = System.getProperty("pradar.data.pusher.pinpoint.collector.address");
        this.dataType = dataType;
        this.version = version;
        this.messageSendService = new PinpointSendServiceFactory().getKafkaMessageInstance();
    }


    @Override
    public void append(String log) {
        stringBuilder.append(log);
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

    @Override
    public void flush() {
        this.send(stringBuilder.toString());
        stringBuilder = new StringBuilder();
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
