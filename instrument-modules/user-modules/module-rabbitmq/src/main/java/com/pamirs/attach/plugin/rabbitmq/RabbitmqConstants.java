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
package com.pamirs.attach.plugin.rabbitmq;

import com.pamirs.pradar.MiddlewareType;

/**
 * @Description
 * @Author xiaobin.zfb
 * @mail xiaobin@shulie.io
 * @Date 2020/8/27 3:57 下午
 */
public final class RabbitmqConstants {
    public final static String MODULE_NAME = "rabbitmq";
    public final static String PLUGIN_NAME = "rabbitmq";
    public final static int PLUGIN_TYPE = MiddlewareType.TYPE_MQ;

    public final static String REFLECT_FIELD_HEADERS = "headers";
    public final static String REFLECT_FIELD_CONSUMER_TAGS = "consumerTags";

    public final static String DYNAMIC_FIELD_ENVELOPE = "_envelope";
    public final static String DYNAMIC_FIELD_BODY = "_body";
    public final static String DYNAMIC_FIELD_PROPERTIES = "_properties";
    public final static String DYNAMIC_FIELD_QUEUE_NAME = "queueName";
    public final static String DYNAMIC_FIELD_QUEUE = "queue";
    public final static String DYNAMIC_FIELD_QUEUES = "queues";

    public final static String DYNAMIC_FIELD_DELEGATE = "delegate";

    public final static String AUTO_ACK_CONFIG = "rabbitmq.autoAck";
    public final static String EXCLUSIVE_CONFIG = "rabbitmq.exclusive";
    public final static String PREFETCH_COUNT_CONFIG = "rabbitmq.prefetchCount";
    public final static String NO_LOCAL_CONFIG = "rabbitmq.noLocal";
    public final static String IS_AUTO_ACK_FIELD = "paradarIsAutoAck";
}
