package com.pamirs.pradar.common;

import com.shulie.instrument.simulator.api.listener.InitializingBean;
import com.shulie.instrument.simulator.api.resource.SimulatorConfig;
import io.shulie.takin.sdk.kafka.MessageSendService;
import io.shulie.takin.sdk.pinpoint.impl.PinpointSendServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;

@Component
public class KafkaSendBuilder implements InitializingBean {
    @Resource
    protected SimulatorConfig simulatorConfig;

    private MessageSendService messageSendService;

    private final static Logger logger = LoggerFactory.getLogger(KafkaSendBuilder.class);

    private PinpointSendServiceImpl buildPinpointSendService(String collectorAddress) {
        try {
            PinpointSendServiceImpl pinpointSendService = new PinpointSendServiceImpl();
            String[] node = collectorAddress.split(":");
            InetSocketAddress socketAddress = new InetSocketAddress(node[0], Integer.parseInt(node[1]));
            pinpointSendService.init(null, null, socketAddress);
            return pinpointSendService;
        } catch (Exception e) {
            logger.error("Parsing collector address failed:{},", collectorAddress, e);
        }
        return null;
    }

    public MessageSendService getMessageSendService() {
        return messageSendService;
    }

    @Override
    public void init() {
        String collectorAddress = simulatorConfig.getProperty("pradar.data.pusher.pinpoint.collector.address", null);
        //优先使用pinpoint，如果两种都没有，不使用kafka发送
        if (collectorAddress != null) {
            messageSendService = buildPinpointSendService(collectorAddress);
        }
    }

}
