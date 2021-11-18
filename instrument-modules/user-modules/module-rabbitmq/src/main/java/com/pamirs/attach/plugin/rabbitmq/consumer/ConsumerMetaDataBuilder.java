package com.pamirs.attach.plugin.rabbitmq.consumer;

import com.pamirs.attach.plugin.rabbitmq.common.DeliverDetail;

/**
 * @author jirenhe | jirenhe@shulie.io
 * @since 2021/11/17 4:05 下午
 */
public interface ConsumerMetaDataBuilder {

    ConsumerMetaData tryBuild(DeliverDetail deliverDetail);
}
