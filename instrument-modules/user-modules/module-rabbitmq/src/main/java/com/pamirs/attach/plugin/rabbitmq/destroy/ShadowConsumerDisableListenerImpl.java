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
package com.pamirs.attach.plugin.rabbitmq.destroy;

import com.pamirs.attach.plugin.rabbitmq.common.ChannelHolder;
import com.pamirs.attach.plugin.rabbitmq.common.ConfigCache;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.Throwables;
import com.pamirs.pradar.pressurement.agent.event.IEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ClusterTestSwitchOffEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.ShadowConsumerDisableEvent;
import com.pamirs.pradar.pressurement.agent.event.impl.SilenceSwitchOnEvent;
import com.pamirs.pradar.pressurement.agent.listener.EventResult;
import com.pamirs.pradar.pressurement.agent.listener.PradarEventListener;
import com.pamirs.pradar.pressurement.agent.listener.ShadowConsumerDisableListener;
import com.pamirs.pradar.pressurement.agent.listener.model.ShadowConsumerDisableInfo;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author angju
 * @date 2021/10/11 10:50
 */
public class ShadowConsumerDisableListenerImpl implements ShadowConsumerDisableListener, PradarEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(ShadowConsumerDisableListenerImpl.class);


    @Override
    public boolean disableBatch(List<ShadowConsumerDisableInfo> list) {
        boolean result = true;
        for (ShadowConsumerDisableInfo shadowConsumerDisableInfo : list) {
            if (!ChannelHolder.getQueueChannel().containsKey(Pradar.addClusterTestPrefix(shadowConsumerDisableInfo.getTopic()))) {
                continue;
            }
            List<Channel> channels = ChannelHolder.getQueueChannel().get(Pradar.addClusterTestPrefix(shadowConsumerDisableInfo.getTopic()));
            for (Channel channel : channels) {
                result = closeChannel(channel, shadowConsumerDisableInfo.getTopic());
            }
        }
        return result;
    }

    @Override
    public boolean disableAll() {
        boolean result = true;
        Map<String, List<Channel>> map = ChannelHolder.getQueueChannel();
        for (Map.Entry<String, List<Channel>> entry : map.entrySet()) {
            List<Channel> channels = entry.getValue();
            if (channels == null) {
                continue;
            }
            for (Channel channel : channels) {
                result = closeChannel(channel, entry.getKey());
            }

        }
        return result;
    }

    private boolean closeChannel(Channel channel, String busQueue) {
        boolean result = true;
        if (channel != null) {
            try {
                channel.close();
                ConfigCache.ConsumerMetaDataCacheKey key = ChannelHolder.shadowChannelWithBizMetaCache.get(channel);
                if (key != null) {
                    ConfigCache.removeConsumerMetaDataCaches(key);
                    ChannelHolder.shadowChannelWithBizMetaCache.remove(channel);
                }
            } catch (IOException e) {
                result = false;
                LOGGER.error("disableBatch rabbitmq IOException ", e);
            } catch (TimeoutException e) {
                result = false;
                LOGGER.error("disableBatch rabbitmq TimeoutException ", e);
            } finally {
                if (result) {
                    try {
                        ChannelHolder.clearOneShadowChannel(channel, busQueue);
                    } catch (Throwable t) {
                        LOGGER.warn("[rabbit-mq]: ", t);
                    }
                }
            }
        } else {
            result = false;
            LOGGER.error("disableBatch rabbitmq,but channel is null");
        }
        return result;
    }

    @Override
    public EventResult onEvent(IEvent event) {
        try {
            if (event instanceof ShadowConsumerDisableEvent) {
                ShadowConsumerDisableEvent shadowConsumerDisableEvent = (ShadowConsumerDisableEvent) event;
                List<ShadowConsumerDisableInfo> list = shadowConsumerDisableEvent.getTarget();
                disableBatch(list);
            } else if (event instanceof ClusterTestSwitchOffEvent) {
                disableAll();
            } else if (event instanceof SilenceSwitchOnEvent) {
                disableAll();
            }
        } catch (Throwable e) {
            EventResult.error(event.getTarget(), e.getMessage());
        }

        return EventResult.success(event.getTarget());
    }

    @Override
    public int order() {
        return 99;
    }
}
